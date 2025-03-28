package com.atguigu.daijia.dispatch.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.dispatch.mapper.OrderJobMapper;
import com.atguigu.daijia.dispatch.service.NewOrderService;
import com.atguigu.daijia.dispatch.xxl.client.XxlJobClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.dispatch.OrderJob;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.atguigu.daijia.common.constant.RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME;

@Slf4j
@Service
public class NewOrderServiceImpl implements NewOrderService {


    @Resource
    private XxlJobClient xxlJobClient;
    @Resource
    private OrderJobMapper orderJobMapper;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;


    // 执行Lua脚本
    @Resource
    private DefaultRedisScript<Long> addDriverOrders;

    @Resource
    private DefaultRedisScript<Long> delDriverOrders;

    // 创建并启动任务调度方法
    @Override
    public Long addAndStartTask(NewOrderTaskVo newOrderTaskVo) {
        Long orderId = newOrderTaskVo.getOrderId();
        OrderJob orderJob = orderJobMapper.selectOne(
                new LambdaQueryWrapper<OrderJob>()
                        .eq(OrderJob::getOrderId, orderId)
                        .select(OrderJob::getJobId)
        );
        if (null == orderJob) {
            // 创建并启动任务调度
            // String executorHandler 执行任务job方法
            // String param
            // String corn 执行cron表达式
            // String desc 描述信息
            Long jobId = xxlJobClient.addJob("newOrderTaskHandler", "",
                    "0 0/1 * * * ?",
                    "新创建订单任务调度：订单=>" + newOrderTaskVo.getOrderId());

            // 记录任务调度信息
            orderJob = new OrderJob();
            orderJob.setOrderId(newOrderTaskVo.getOrderId());
            orderJob.setJobId(jobId);
            orderJob.setParameter(JSON.toJSONString(newOrderTaskVo));
            orderJobMapper.insert(orderJob);
            xxlJobClient.startJob(jobId);
        }
        return orderJob.getJobId();
    }

    @Override
    public void executeTask(long jobId) {

        // 1.查询任务
        LambdaQueryWrapper<OrderJob> wrapper = new LambdaQueryWrapper<OrderJob>()
                .select(BaseEntity::getId, OrderJob::getJobId, OrderJob::getOrderId, OrderJob::getParameter)
                .eq(OrderJob::getJobId, jobId);
        OrderJob orderJob = orderJobMapper.selectOne(wrapper);
        if (orderJob == null) {
            // 停止并删除任务调度
            xxlJobClient.removeJob(jobId);
            return;
        }

        // 2.查询订单数据~
        String orderJson = orderJob.getParameter();
        if (StringUtils.isBlank(orderJson)) {
            // 停止并删除任务调度
            xxlJobClient.removeJob(jobId);
            return;
        }
        NewOrderTaskVo newOrderTaskVo;
        try {
            newOrderTaskVo = JSON.parseObject(orderJson, NewOrderTaskVo.class);
        } catch (Exception e) {
            e.printStackTrace();
            // 停止并删除任务调度
            xxlJobClient.removeJob(jobId);
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            // 停止并删除任务调度
        }


        long time = newOrderTaskVo.getCreateTime().getTime() + DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME * 1000;
        // 超过15分钟自动取消订单
        Long orderId = newOrderTaskVo.getOrderId();
        if (System.currentTimeMillis() > time) {
            Result<Boolean> updateOrderStatus = orderInfoFeignClient.updateOrderStatus(orderId,
                    OrderStatus.ORDER_TIMEOUT.getStatus());
            updateOrderStatus.throwOnFailure();
            // 停止并删除任务调度
            xxlJobClient.removeJob(jobId);
            return;

        }

        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        orderStatus.throwOnFailureOrDataIsNull();
        Integer status = orderStatus.getData();
        if (!OrderStatus.WAITING_ACCEPT.getStatus().equals(status)) {
            // 停止并删除任务调度
            xxlJobClient.removeJob(jobId);
            // 删除订单在缓存的信息
            // stringRedisTemplate.opsForSet().remove(repeatKey);
            return;
        }

        // 3.查找附件的老司机
        SearchNearByDriverForm searchNearByDriverForm = new SearchNearByDriverForm();
        searchNearByDriverForm.setLongitude(newOrderTaskVo.getStartPointLongitude());
        searchNearByDriverForm.setLatitude(newOrderTaskVo.getStartPointLatitude());
        searchNearByDriverForm.setMileageDistance(newOrderTaskVo.getExpectDistance());
        Result<List<NearByDriverVo>> nearByDriverRes = locationFeignClient.searchNearByDriver(searchNearByDriverForm);
        nearByDriverRes.throwOnFailure();

        List<NearByDriverVo> data = nearByDriverRes.getData();

        if (!CollectionUtils.isEmpty(data)) {
            data.forEach(driver -> {
                Long driverId = driver.getDriverId();
                // 记录司机id，防止重复推送
                String key = RedisConstant.DRIVER_ORDER_INFO_HASH + driverId;
                Boolean isMember = stringRedisTemplate.opsForHash().hasKey(key, orderId.toString());
                if (Boolean.FALSE.equals(isMember)) {
                    // 把订单信息推送给满足条件多个司机
                    // stringRedisTemplate.opsForSet().add(repeatKey, driverId.toString());
                    NewOrderDataVo newOrderDataVo = new NewOrderDataVo();
                    newOrderDataVo.setOrderId(newOrderTaskVo.getOrderId());
                    newOrderDataVo.setStartLocation(newOrderTaskVo.getStartLocation());
                    newOrderDataVo.setEndLocation(newOrderTaskVo.getEndLocation());
                    newOrderDataVo.setExpectAmount(newOrderTaskVo.getExpectAmount());
                    newOrderDataVo.setExpectDistance(newOrderTaskVo.getExpectDistance());
                    newOrderDataVo.setExpectTime(newOrderTaskVo.getExpectTime());
                    newOrderDataVo.setFavourFee(newOrderTaskVo.getFavourFee());
                    newOrderDataVo.setDistance(driver.getDistance());
                    newOrderDataVo.setCreateTime(newOrderTaskVo.getCreateTime());

                    // String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driver.getDriverId();

                    Long execute = stringRedisTemplate.execute(addDriverOrders,
                            Collections.emptyList(),
                            orderId.toString(),
                            driverId.toString(),
                            String.valueOf(newOrderDataVo.getCreateTime().getTime()),
                            JSON.toJSONString(newOrderDataVo),
                            String.valueOf(DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME)
                    );
                    log.info("订单加入到司机缓存中结果={}", execute);
                    // 新订单保存司机的临时队列，Redis里面队列集合
                    // Long result = stringRedisTemplate.execute(
                    //         new DefaultRedisScript<>(script, Long.class),
                    //         Collections.singletonList(key),
                    //         newOrderTaskVo.getCreateTime().getTime(),
                    //         JSON.toJSONString(newOrderDataVo),
                    //         String.valueOf(DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME)
                    // );


                    // stringRedisTemplate.opsForZSet().add(key,
                    //         JSON.toJSONString(newOrderDataVo),
                    //         System.currentTimeMillis());

                    // stringRedisTemplate.opsForList().leftPush(key, JSON.toJSONString(newOrderDataVo));
                    // 过期时间：15分钟
                    // stringRedisTemplate.expire(key, RedisConstant.DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME,
                    //         TimeUnit.MINUTES);
                }


            });
        }

        // 过期时间：15分钟，超过15分钟没有接单自动取消
        // stringRedisTemplate.expire(repeatKey,
        //         RedisConstant.DRIVER_ORDER_REPEAT_LIST_EXPIRES_TIME,
        //         TimeUnit.MINUTES);
    }


    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        String key = RedisConstant.DRIVER_ORDER_ID_ZSET + driverId;
        String k2 = RedisConstant.DRIVER_ORDER_INFO_HASH + driverId;

        long min = System.currentTimeMillis() - DRIVER_ORDER_TEMP_LIST_EXPIRES_TIME * 1000;

        Set<String> orderIdsJson = stringRedisTemplate.opsForZSet().rangeByScore(key, min, System.currentTimeMillis());
        if (CollectionUtils.isEmpty(orderIdsJson)) {
            return Collections.emptyList();

        }
        // // 查看最近的订单
        // List<Long> orderIds = orderIdsJson.stream().map(order -> JSON.parseObject(order,
        //         Long.class)).toList();


        // List<NewOrderDataVo> newOrderDataVos = objects.stream().map(item -> {
        //     String s = JSON.
        //     JSONObject jsonObject = JSON.parseObject(s);
        //     return jsonObject.toJavaObject(NewOrderDataVo.class);

        // }).toList();

        // 查看最近的订单
        // List<String> nearOrders = stringRedisTemplate.opsForList().range(key, 0, -1);
        // if (CollectionUtils.isEmpty(nearOrders)) return Collections.emptyList();
        // return nearOrders.stream().map(order -> JSON.parseObject(order,
        //         NewOrderDataVo.class)).toList();
        // return newOrderDataVos;

        List<Object> objects = stringRedisTemplate.opsForHash().multiGet(k2, new ArrayList<>(orderIdsJson));
        List<NewOrderDataVo> newOrderDataVos = JSON.parseArray(objects.toString(), NewOrderDataVo.class);
        return newOrderDataVos;
    }

    @Override
    public Boolean clearNewOrderQueueData(Long driverId) {
        // String key = RedisConstant.DRIVER_ORDER_TEMP_LIST + driverId;
        // Boolean delete = stringRedisTemplate.delete(key);
        Long execute = stringRedisTemplate.execute(delDriverOrders, Collections.emptyList(),
                driverId.toString());
        return execute != null && execute > 0;
    }
}
