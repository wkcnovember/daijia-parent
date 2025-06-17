package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.IdUtils;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.order.*;
import com.atguigu.daijia.model.enums.order.OrderStatus;
import com.atguigu.daijia.model.enums.order.ProfitSharingStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.query.order.OrderCount;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.handle.OrderDelayService;
import com.atguigu.daijia.order.mapper.OrderBillMapper;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderProfitsharingMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.atguigu.daijia.order.service.OrderMonitorService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import static com.atguigu.daijia.common.constant.RedisConstant.ORDER_ACCEPT_MARK;
import static com.atguigu.daijia.common.constant.RedisConstant.ORDER_ACCEPT_MARK_EXPIRES_TIME;

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

    @Resource(name = "orderIdSuitableDriverIds")
    private DefaultRedisScript<Long> orderIdSuitableDriverIds;

    @Resource
    private OrderMonitorService orderMonitorService;

    @Resource
    private OrderBillMapper orderBillMapper;


    @Resource
    private OrderProfitsharingMapper orderProfitsharingMapper;


    @Resource
    private OrderDelayService orderDelayService;


    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = orderInfoConvert.toOrderInfo(orderInfoForm);
        // 订单号
        String orderNo = IdUtils.fastSimpleUUID();
        orderInfo.setOrderNo(orderNo);
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        save(orderInfo);
        // 生成订单之后，发送延迟消息(处理订单超时)
        orderDelayService.addOrderToDelayQueue(orderInfo.getId().toString());

        // 记录日志
        log(orderInfo.getId(), orderInfo.getStatus());
        Long orderId = orderInfo.getId();
        // 向redis添加标识
        // 接单标识，标识不存在了说明不在等待接单状态了
        stringRedisTemplate.execute(orderIdSuitableDriverIds,
                Collections.emptyList(),
                orderId.toString(),
                "0",
                String.valueOf(ORDER_ACCEPT_MARK_EXPIRES_TIME)
        );


        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId, Long id, boolean isDriver) {
        LambdaQueryWrapper<OrderInfo> eq = new LambdaQueryWrapper<OrderInfo>()
                .select(OrderInfo::getStatus)
                .eq(BaseEntity::getId, orderId);

        if (isDriver) {
            eq.eq(OrderInfo::getDriverId, id);
        } else {
            eq.eq(OrderInfo::getCustomerId, id);
        }

        OrderInfo orderInfo =
                baseMapper.selectOne(eq);
        if (orderInfo == null) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        return orderInfo.getStatus();
    }

    /**
     * 修改订单状态订单
     *
     * @return
     */
    @Override
    public Boolean updateOrderStatus(Long orderId, Integer status) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setId(orderId);
        orderInfo.setStatus(status);
        return updateById(orderInfo);
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

            Boolean isMember = stringRedisTemplate.opsForSet().isMember(ORDER_ACCEPT_MARK + orderId,
                    driverId.toString());


            if (Boolean.FALSE.equals(isMember)) {
                // 清楚脏数据
                delOrderZsetAndHash(driverId, orderId);
                // 抢单失败
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);

            }

            // 最终校验订单的合法性
            LambdaQueryWrapper<OrderInfo> wrapper =
                    new LambdaQueryWrapper<OrderInfo>()
                            .select(BaseEntity::getId,
                                    OrderInfo::getCustomerId,
                                    OrderInfo::getStatus)
                            .eq(BaseEntity::getId, orderId)
                            .eq(OrderInfo::getStatus, OrderStatus.WAITING_ACCEPT.getStatus())
                            .isNull(OrderInfo::getDriverId);
            OrderInfo orderInfo = baseMapper.selectOne(wrapper);
            if (orderInfo == null) {
                log.warn("此订单不存在或已经被抢~");
                // 清楚脏数据
                delOrderZsetAndHash(driverId, orderId);
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);

            }
            // 修改订单信息
            orderInfo.setDriverId(driverId);
            orderInfo.setAcceptTime(LocalDateTime.now());
            orderInfo.setStatus(OrderStatus.ACCEPTED.getStatus());
            boolean isSuccess = updateById(orderInfo);
            if (!isSuccess) {
                // 清楚脏数据
                delOrderZsetAndHash(driverId, orderId);
                throw new GuiguException(ResultCodeEnum.NOT_EXISTS_ORDER);
            }

            // 记录日志
            log(orderId, orderInfo.getStatus());

            // 删除订单对应的标示
            stringRedisTemplate.unlink(ORDER_ACCEPT_MARK + orderId);

            return Boolean.TRUE;

        } catch (GuiguException e) {
            // 抢单失败
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("司机抢单业务出现异常情况={}", e);
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        } finally {
            // 改进点，只能删除属于自己的key，不能删除别人的
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

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
        updateOrderInfo.setArriveTime(LocalDateTime.now());
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

        // if (Boolean.TRUE.equals(isFlag)) {
        //     CompletableFuture.runAsync(() -> {
        //         stringRedisTemplate.expire(ORDER_DRIVER_CUSTOMER_HASH + orderId, ORDER_DRIVER_CUSTOMER_TIMEOUT,
        //                 TimeUnit.MINUTES);
        //     }, sharedThreadPool);
        //
        // }
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
        updateOrderInfo.setStartServiceTime(LocalDateTime.now());
        // 只能更新自己的订单
        int row = baseMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 记录日志
            log(startDriveForm.getOrderId(), OrderStatus.START_SERVICE.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        // 初始化订单监控统计数据
        OrderMonitor orderMonitor = new OrderMonitor();
        orderMonitor.setOrderId(startDriveForm.getOrderId());
        orderMonitorService.saveOrderMonitor(orderMonitor);

        return Boolean.TRUE;
    }

    @Override
    public Long getOrderNumByTime(OrderCount orderCount) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper
                .eq(OrderInfo::getDriverId, orderCount.getDriverId())
                .between(OrderInfo::getStartServiceTime, orderCount.getStartServiceTime(),
                        orderCount.getEndServiceTime());
        Long count = baseMapper.selectCount(wrapper);
        return count;
    }

    @Override
    @Transactional
    public Boolean endDrive(UpdateOrderBillForm updateOrderBillForm) {
        // 1 更新订单信息
        // update order_info set ..... where id=? and driver_id=?
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OrderInfo::getId, updateOrderBillForm.getOrderId());
        wrapper.eq(OrderInfo::getDriverId, updateOrderBillForm.getDriverId());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setStatus(OrderStatus.END_SERVICE.getStatus());
        orderInfo.setRealAmount(updateOrderBillForm.getTotalAmount());
        orderInfo.setFavourFee(updateOrderBillForm.getFavourFee());
        orderInfo.setRealDistance(updateOrderBillForm.getRealDistance());
        orderInfo.setEndServiceTime(LocalDateTime.now());
        int rows = baseMapper.update(orderInfo, wrapper);

        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        OrderBill orderBill = orderInfoConvert.toOrderBill(updateOrderBillForm);

        orderBill.setRewardRuleId(updateOrderBillForm.getRewardRuleId());
        // 添加账单数据
        rows = orderBillMapper.insert(orderBill);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        // 添加分账信息
        OrderProfitsharing orderProfitsharing = orderInfoConvert.toOrderProfitsharing(updateOrderBillForm);
        orderProfitsharing.setRuleId(updateOrderBillForm.getProfitsharingRuleId());
        orderProfitsharing.setStatus(ProfitSharingStatus.OFF.getStatus());
        rows = orderProfitsharingMapper.insert(orderProfitsharing);
        if (rows != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }

        return Boolean.TRUE;
    }

    @Override
    public PageVo<OrderListVo> findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId) {
        // 状态 < 已付款 预估金额  >=  实际总付款bill
        IPage<OrderListVo> orderInfoPageVo = baseMapper.selectCustomerOrderPage(pageParam, customerId);
        return PageVo.toPageVo(orderInfoPageVo);
    }

    @Override
    public PageVo<OrderListVo> findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId) {
        // 状态 < 已付款 预估金额  >=  实际总付款bill
        IPage<OrderListVo> orderInfoPageVo = baseMapper.selectDriverOrderPage(pageParam, driverId);
        return PageVo.toPageVo(orderInfoPageVo);
    }

    @Override
    public OrderBillVo getOrderBillInfo(Long orderId) {
        OrderBill orderBill = orderBillMapper.selectOne(new LambdaQueryWrapper<OrderBill>().eq(OrderBill::getOrderId,
                orderId));
        return orderInfoConvert.toOrderBillVo(orderBill);
    }

    @Override
    public OrderProfitsharingVo getOrderProfitSharing(Long orderId) {
        OrderProfitsharing orderProfitsharing =
                orderProfitsharingMapper.selectOne(new LambdaQueryWrapper<OrderProfitsharing>().eq(OrderProfitsharing::getOrderId, orderId));
        return orderInfoConvert.toOrderProfitsharingVo(orderProfitsharing);
    }

    @Override
    public Boolean sendOrderBillInfo(Long orderId, Long driverId) {
        // 更新订单信息
        LambdaQueryWrapper<OrderInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(OrderInfo::getId, orderId);
        queryWrapper.eq(OrderInfo::getDriverId, driverId);
        // 更新字段
        OrderInfo updateOrderInfo = new OrderInfo();
        updateOrderInfo.setStatus(OrderStatus.UNPAID.getStatus());
        // 只能更新自己的订单
        int row = baseMapper.update(updateOrderInfo, queryWrapper);
        if (row == 1) {
            // 记录日志
            this.log(orderId, OrderStatus.UNPAID.getStatus());
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }

    @Override
    public OrderPayVo getOrderPayVo(String orderNo, Long customerId) {
        OrderPayVo orderPayVo = baseMapper.selectOrderPayVo(orderNo, customerId);
        if (null != orderPayVo) {
            String content = orderPayVo.getStartLocation() + " 到 " + orderPayVo.getEndLocation();
            orderPayVo.setContent(content);
        }
        return orderPayVo;
    }

    @Override
    @Transactional
    public Boolean updateOrderPayStatus(String orderNo) {
        // 1 根据订单编号查询，判断订单状态
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper
                .select(OrderInfo::getStatus, BaseEntity::getId)
                .eq(OrderInfo::getOrderNo, orderNo);
        OrderInfo orderInfo = baseMapper.selectOne(wrapper);
        if (orderInfo == null || Objects.equals(orderInfo.getStatus(), OrderStatus.PAID.getStatus())) {
            return Boolean.TRUE;
        }

        // 2 更新状态
        orderInfo.setStatus(OrderStatus.PAID.getStatus());
        orderInfo.setPayTime(LocalDateTime.now());

        int rows = baseMapper.updateById(orderInfo);

        if (rows == 1) {
            return Boolean.TRUE;
        } else {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }

    @Override
    public OrderRewardVo getOrderRewardFee(String orderNo) {
        OrderRewardVo orderRewardVo = baseMapper.getOrderRewardFee(orderNo);
        return orderRewardVo;
    }

    // todo 判断状态
    @Override
    public Boolean customerCancelNoAcceptOrder(Long customerId, Long orderId) {
        LambdaUpdateWrapper<OrderInfo> eq = new LambdaUpdateWrapper<OrderInfo>()
                .set(OrderInfo::getStatus, OrderStatus.CUSTOMER_CANCEL_ORDER.getStatus())
                .eq(BaseEntity::getId, orderId)
                .eq(OrderInfo::getCustomerId, customerId);
        boolean isSuccess = update(eq);
        if (!isSuccess) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        stringRedisTemplate.delete(ORDER_ACCEPT_MARK + orderId);
        return Boolean.TRUE;
    }

    /**
     * 取消订单业务逻辑
     */
    @Override
    public void orderCancel(Long orderId) {
        // orderId查询订单信息
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<OrderInfo>()
                .select(BaseEntity::getId, OrderInfo::getStatus)
                .eq(BaseEntity::getId, orderId);
        OrderInfo orderInfo = baseMapper.selectOne(wrapper);
        if (orderInfo == null || !Objects.equals(OrderStatus.WAITING_ACCEPT.getStatus(), orderInfo.getStatus())) {
            return;
        }
        // 修改订单状态：取消状态
        orderInfo.setStatus(OrderStatus.CANCEL_ORDER.getStatus());
        baseMapper.updateById(orderInfo);

        // 删除接单标识

        stringRedisTemplate.delete(RedisConstant.ORDER_ACCEPT_MARK + orderId);
    }

    @Override
    public Boolean isStartDrive(Long driverId, Long orderId) {
        LambdaUpdateWrapper<OrderInfo> eq = new LambdaUpdateWrapper<OrderInfo>()
                .eq(BaseEntity::getId, orderId)
                .eq(OrderInfo::getDriverId, driverId)
                .eq(OrderInfo::getStatus, OrderStatus.START_SERVICE.getStatus());

        return count(eq) == 1;
    }

    @Transactional
    @Override
    public Boolean updateCouponAmount(Long orderId, BigDecimal couponAmount) {
        int row = orderBillMapper.updateCouponAmount(orderId, couponAmount);
        if (row != 1) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }

    @Override
    public Boolean isCustomerCurrentOrder(Long customerId, Long orderId) {
        Boolean currentOrder = isCurrentOrder(true, customerId, orderId);
        return currentOrder;
    }


    private Boolean isCurrentOrder(boolean isCustomer, Long id, Long orderId) {
        LambdaQueryWrapper<OrderInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BaseEntity::getId, orderId);
        if (isCustomer) {
            wrapper.eq(OrderInfo::getCustomerId, id);
        } else {
            wrapper.eq(OrderInfo::getDriverId, id);
        }
        long count = count(wrapper);
        return count == 1;

        // Map<Object, Object> entries =
        //         stringRedisTemplate.opsForHash().entries(ORDER_DRIVER_CUSTOMER_HASH + orderId);
        // if (CollectionUtils.isEmpty(entries)) {
        //     return Boolean.FALSE;
        // }
        // String jsonString = JSON.toJSONString(entries);
        // DcId dcId = JSON.parseObject(jsonString, DcId.class);
        // if (isCustomer) {
        //     return Objects.equals(dcId.getCustomerId(), id);
        // }
        //
        // return Objects.equals(dcId.getDriverId(), id);

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
