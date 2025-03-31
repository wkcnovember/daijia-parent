package com.atguigu.daijia.order.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.redis.DcId;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.atguigu.daijia.common.constant.RedisConstant.ORDER_DRIVER_CUSTOMER_HASH;
import static com.atguigu.daijia.common.constant.RedisConstant.ORDER_DRIVER_CUSTOMER_TIMEOUT;

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

    @Resource(name = "delDriverOrders")
    private DefaultRedisScript<Long> delDriverOrderKeys;

    @Resource(name = "orderDcMapping")
    private DefaultRedisScript<Long> orderDcMapping;

    @Resource
    private OrderMonitorService orderMonitorService;

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
        // 保存订单与乘客映射关系
        stringRedisTemplate.execute(orderDcMapping, Collections.emptyList(),
                orderId.toString(),
                "",
                orderInfo.getCustomerId().toString(),
                String.valueOf(ORDER_DRIVER_CUSTOMER_TIMEOUT)

        );


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

            String key = RedisConstant.DRIVER_ORDER_INFO_HASH + driverId;
            Boolean isExists = stringRedisTemplate.opsForHash().hasKey(
                    key, orderId.toString());

            if (Boolean.FALSE.equals(isExists)) {
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);
            }
            // 校验订单是否被不处于等待状态了
            LambdaQueryWrapper<OrderInfo> wrapper =
                    new LambdaQueryWrapper<OrderInfo>()
                            .select(BaseEntity::getId,
                                    OrderInfo::getCustomerId,
                                    OrderInfo::getStatus)
                            .eq(BaseEntity::getId, orderId)
                            .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus());
            OrderInfo orderInfo = baseMapper.selectOne(wrapper);
            if (orderInfo == null) {
                // 清楚脏数据
                delOrderZsetAndHash(driverId, orderId);
                log.warn("此订单不存在~");
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);

            }
            // 订单不在接单状态 删除redis对应的order缓存
            if (!Objects.equals(OrderStatus.WAITING_ACCEPT.getStatus(), orderInfo.getStatus())) {
                log.warn("此订单处于非等待状态~");
                // 清楚脏数据
                delOrderZsetAndHash(driverId, orderId);
            }
            // 修改订单信息
            orderInfo.setDriverId(driverId);
            orderInfo.setAcceptTime(new Date());
            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
            boolean isSuccess = updateById(orderInfo);
            if (!isSuccess) {
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);
            }

            // 添加 订单与司机和乘客的标示
            stringRedisTemplate.execute(orderDcMapping, Collections.emptyList(),
                    orderId.toString(),
                    driverId.toString(),
                    orderInfo.getCustomerId().toString(),
                    String.valueOf(ORDER_DRIVER_CUSTOMER_TIMEOUT)
            );


            // 删除订单标识位
            // delOrderZsetAndHash(driverId, orderId);


            // 记录日志
            log(orderId, orderInfo.getStatus());
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

    private void delOrderZsetAndHash(Long driverId, Long orderId) {
        Long execute = stringRedisTemplate.execute(delDriverOrderKeys,
                Collections.emptyList(),
                orderId.toString(),
                driverId.toString());
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId) {
        return getCurrentOrderByCIdOrDId(true, customerId);
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        return getCurrentOrderByCIdOrDId(false, driverId);
    }

    @Override
    public OrderInfo getOrderInfo(Long orderId) {
        return getById(orderId);
    }

    @Override
    @Transactional
    public Boolean driverArriveStartLocation(Long orderId, Long driverId) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseEntity::getId, orderId)
                .eq(OrderInfo::getDriverId, driverId);
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.DRIVER_ARRIVED.getStatus());
        updateOrderInfo.setArriveTime(new Date());
        // 只能更新自己的订单
        int row = baseMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 记录日志
            log(orderId, OrderStatus.DRIVER_ARRIVED.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }

    @Transactional
    @Override
    public Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, updateOrderCartForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, updateOrderCartForm.getDriverId());

        OrderInfo updateOrderInfo = new OrderInfo();
        BeanUtils.copyProperties(updateOrderCartForm, updateOrderInfo);
        updateOrderInfo.setStatus(OrderStatus.UPDATE_CART_INFO.getStatus());
        // 只能更新自己的订单
        int row = baseMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 记录日志
            this.log(updateOrderCartForm.getOrderId(), OrderStatus.UPDATE_CART_INFO.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean isDriverCurrentOrder(Long driverId, Long orderId) {
        Boolean isFlag = isCurrentOrder(false, driverId, orderId);
        if (Boolean.TRUE.equals(isFlag)) {
            stringRedisTemplate.expire(ORDER_DRIVER_CUSTOMER_HASH + orderId, ORDER_DRIVER_CUSTOMER_TIMEOUT,
                    TimeUnit.MINUTES);
        }
        return isFlag;
    }

    @Transactional
    @Override
    public Boolean startDrive(StartDriveForm startDriveForm) {
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, startDriveForm.getOrderId());
        queryWrapper.eq(OrderInfo::getDriverId, startDriveForm.getDriverId());

        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.START_SERVICE.getStatus());
        updateOrderInfo.setStartServiceTime(new Date());
        // 只能更新自己的订单
        int row = baseMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 记录日志
            log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        //初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);
        return Boolean.TRUE;
    }

    @Override
    public Boolean isCustomerCurrentOrder(Long customerId, Long orderId) {
        Boolean currentOrder = isCurrentOrder(true, customerId, orderId);
        if (Boolean.TRUE.equals(currentOrder)) {
            stringRedisTemplate.expire(ORDER_DRIVER_CUSTOMER_HASH + orderId, ORDER_DRIVER_CUSTOMER_TIMEOUT,
                    TimeUnit.MINUTES);
        }
        return currentOrder;
    }


    private Boolean isCurrentOrder(boolean isCustomer, Long id, Long orderId) {
        Map<Object, Object> entries =
                stringRedisTemplate.opsForHash().entries(ORDER_DRIVER_CUSTOMER_HASH + orderId);
        if (CollectionUtils.isEmpty(entries)) {
            return Boolean.FALSE;
        }
        String jsonString = JSON.toJSONString(entries);
        DcId dcId = JSON.parseObject(jsonString, DcId.class);
        if (isCustomer) {
            return Objects.equals(dcId.getCustomerId(),id);
        }

        return Objects.equals(dcId.getDriverId(),id);

    }


    private CurrentOrderInfoVo getCurrentOrderByCIdOrDId(boolean isCustomer, Long id) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(BaseEntity::getId, OrderInfo::getStatus);

        if (isCustomer) {
            wrapper.eq(OrderInfo::getCustomerId, id);
        } else {
            wrapper.eq(OrderInfo::getDriverId, id);
        }

        wrapper.between(OrderInfo::getStatus, OrderStatus.ACCEPTED.getStatus(), OrderStatus.UNPAID.getStatus());
        wrapper.last("limit 1");
        OrderInfo orderInfo = getOne(wrapper);
        CurrentOrderInfoVo currentOrderInfoVo = new CurrentOrderInfoVo();
        if (orderInfo != null) {
            currentOrderInfoVo.setOrderId(orderInfo.getId());
            currentOrderInfoVo.setStatus(orderInfo.getStatus());
            currentOrderInfoVo.setIsHasCurrentOrder(Boolean.TRUE);
        } else {
            currentOrderInfoVo.setIsHasCurrentOrder(Boolean.FALSE);
        }

        return currentOrderInfoVo;
    }


    void log(long orderId, Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
