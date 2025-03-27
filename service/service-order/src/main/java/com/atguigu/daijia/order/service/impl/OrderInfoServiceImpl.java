package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.atguigu.daijia.common.constant.RedisConstant.DRIVER_ORDER_ID_ZSET;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {


    @Resource
    private OrderInfoConvert orderInfoConvert;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private RedissonClient redissonClient;

    @Resource
    private DefaultRedisScript<Long> delDriverOrders;

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = orderInfoConvert.toOrderInfo(orderInfoForm);
        // 订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-", "");
        orderInfo.setOrderNo(orderNo);
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        save(orderInfo);
        log(orderInfo.getId(), orderInfo.getStatus());
        Long orderId = orderInfo.getId();
        // 向redis添加标识
        // 接单标识，标识不存在了说明不在等待接单状态了  无需~ 在任务调度已经占坑了
        // stringRedisTemplate.opsForValue().set(RedisConstant.ORDER_ACCEPT_MARK + orderId,
        //         "", RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME, TimeUnit.MINUTES);
        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        OrderInfo orderInfo =
                baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(BaseEntity::getId, orderId).select(OrderInfo::getStatus));
        if (orderInfo == null) return OrderStatus.NULL_ORDER.getStatus();

        return orderInfo.getStatus();
    }

    /**
     * 修改订单状态订单
     *
     * @param orderId
     * @param status
     * @return
     */
    @Override
    public Boolean updateOrderStatus(Long orderId, Integer status) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setStatus(status);
        boolean b = updateById(orderInfo);
        return b;
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {


        // 0.0. 创建锁
        final RLock lock = redissonClient.getLock(RedisConstant.ROB_NEW_ORDER_LOCK + orderId);


        try {
            // 获取锁
            boolean flag = lock.tryLock(RedisConstant.ROB_NEW_ORDER_LOCK_WAIT_TIME,
                    RedisConstant.ROB_NEW_ORDER_LOCK_LEASE_TIME, TimeUnit.SECONDS);
            if (!flag) {
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.COB_NEW_ORDER_FAIL);
            }
            // 1.此订单是否存在 + 属于用户的订单吗?
            // String repeatKey =
            //         RedisConstant.DRIVER_ORDER_REPEAT_LIST + orderId;

            String key = RedisConstant.DRIVER_ORDER_INFO_HASH + driverId;
            Boolean isExists = stringRedisTemplate.opsForHash().hasKey(
                    key, orderId.toString());
            // .isMember(repeatKey, driverId.toString());

            if (Boolean.FALSE.equals(isExists)) {
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);
            }


            // 校验订单是否被不处于等待状态了
            LambdaQueryWrapper<OrderInfo> wrapper =
                    new LambdaQueryWrapper<OrderInfo>()
                            .select(BaseEntity::getId, OrderInfo::getStatus)
                            .eq(BaseEntity::getId, orderId)
                            .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus());
            OrderInfo orderInfo = baseMapper.selectOne(wrapper);
            if (orderInfo == null) {
                Long execute = stringRedisTemplate.execute(delDriverOrders,
                        Collections.emptyList(),
                        orderId.toString(),
                        driverId.toString());
                log.warn("此订单处于非等待状态~");
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);

            }


            // 订单不在接单状态 删除redis对应的order缓存
            // if (!Objects.equals(OrderStatus.WAITING_ACCEPT.getStatus(), orderInfo.getStatus())) {
            //     stringRedisTemplate.opsForHash().delete(key, orderId.toString());
            //     String key2 = DRIVER_ORDER_ID_ZSET + driverId;
            //     stringRedisTemplate.opsForZSet().remove(key2,orderId.toString());
            //     throw new GuiguException(ResultCodeEnum.CANCEL_ORDER);
            // }
            // 修改订单信息
            orderInfo = new OrderInfo();
            orderInfo.setId(orderId);
            orderInfo.setDriverId(driverId);
            orderInfo.setAcceptTime(new Date());
            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
            boolean isSuccess = updateById(orderInfo);
            if (!isSuccess) {
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);
            }
            // 删除订单标识位
            stringRedisTemplate.opsForHash().delete(key, orderId.toString());

            return Boolean.TRUE;

        } catch (GuiguException e) {
            // 抢单失败
            throw new GuiguException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            log.error("司机抢单业务出现异常情况={}", e);
        } finally {
            // 改进点，只能删除属于自己的key，不能删除别人的
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        return Boolean.FALSE;
    }

    void log(long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
