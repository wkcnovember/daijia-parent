package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.coupon.client.CouponFeignClient;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.map.client.WxPayFeignClient;
import com.atguigu.daijia.model.convert.map.CalculateDrivingLineConvert;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.order.OrderStatus;
import com.atguigu.daijia.model.enums.payment.PayType;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.payment.CreateWxPaymentForm;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.model.vo.payment.WxPrepayVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {


    @Resource
    private MapFeignClient mapFeignClient;
    @Resource
    private FeeRuleFeignClient feeRuleFeignClient;

    @Resource
    private CalculateDrivingLineConvert calculateDrivingLineConvert;
    @Resource
    private OrderInfoConvert orderInfoConvert;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;

    @Resource
    private ThreadPoolExecutor sharedThreadPool;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;

    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private LocationFeignClient locationFeignClient;

    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Resource
    private WxPayFeignClient wxPayFeignClient;

    @Resource
    private CouponFeignClient couponFeignClient;

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {


        // 预估驾驶路线
        CalculateDrivingLineForm calculateDrivingLineForm =
                calculateDrivingLineConvert.toCalculateDrivingLine(expectOrderForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        DrivingLineVo drivingLineVo = drivingLineVoResult.throwOnFailureOrDataIsNull().getData();
        expectOrderVo.setDrivingLineVo(drivingLineVo);

        // 预估订单金额

        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(LocalDateTime.now());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo data = feeRuleResponseVoResult.throwOnFailureOrDataIsNull().getData();
        expectOrderVo.setFeeRuleResponseVo(data);

        return expectOrderVo;
    }


    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {


        // 1 重新计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm =
                calculateDrivingLineConvert.toCalculateDrivingLineBySb(submitOrderForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);


        DrivingLineVo drivingLineVo = drivingLineVoResult.throwOnFailureOrDataIsNull().getData();
        // 2 重新订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        BigDecimal distance = drivingLineVo.getDistance();
        feeRuleRequestForm.setDistance(distance);
        LocalDateTime startTime = LocalDateTime.now();
        feeRuleRequestForm.setStartTime(startTime);
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.throwOnFailureOrDataIsNull().getData();

        // 3.封装数据订单
        OrderInfoForm orderInfoForm = orderInfoConvert.toOrderInfoForm(submitOrderForm);
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        orderInfoForm.setExpectDistance(distance);

        // 4.远程调用订单添加接口~
        Result<Long> longResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);

        Long orderId = longResult.throwOnFailureOrDataIsNull().getData();


        // 任务调度：查询附近可以接单司机
        CompletableFuture.runAsync(() -> {
            NewOrderTaskVo newOrderDispatchVo = new NewOrderTaskVo();
            newOrderDispatchVo.setOrderId(orderId);
            newOrderDispatchVo.setStartLocation(orderInfoForm.getStartLocation());
            newOrderDispatchVo.setStartPointLongitude(orderInfoForm.getStartPointLongitude());
            newOrderDispatchVo.setStartPointLatitude(orderInfoForm.getStartPointLatitude());
            newOrderDispatchVo.setEndLocation(orderInfoForm.getEndLocation());
            newOrderDispatchVo.setEndPointLongitude(orderInfoForm.getEndPointLongitude());
            newOrderDispatchVo.setEndPointLatitude(orderInfoForm.getEndPointLatitude());
            newOrderDispatchVo.setExpectAmount(orderInfoForm.getExpectAmount());
            newOrderDispatchVo.setExpectDistance(orderInfoForm.getExpectDistance());
            newOrderDispatchVo.setExpectTime(drivingLineVo.getDuration());
            newOrderDispatchVo.setFavourFee(orderInfoForm.getFavourFee());
            newOrderDispatchVo.setCreateTime(startTime);
            // 远程调用
            newOrderFeignClient.addAndStartTask(newOrderDispatchVo);
            // Long jobId = newOrderFeignClient.addAndStartTask(newOrderDispatchVo).getData();
        }, sharedThreadPool);


        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        Integer status =
                orderInfoFeignClient.getCustomerOrderStatus(customerId, orderId).throwOnFailureOrDataIsNull().getData();
        return status;
    }

    @Override
    public Boolean customerCancelNoAcceptOrder(Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        Result<Boolean> isValidateResult = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        isValidateResult.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(isValidateResult.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Boolean> result = orderInfoFeignClient.customerCancelNoAcceptOrder(customerId,
                orderId);
        return result.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public CurrentOrderInfoVo searchCustomerCurrentOrder(Long userId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchCustomerCurrentOrder(userId);
        currentOrderInfoVoResult.throwOnFailureOrDataIsNull();
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long customerId) {
        OrderInfo orderInfo = orderInfoFeignClient.getOrderInfo(orderId)
                .throwOnFailureOrDataIsNull().getData();
        // 必须是自己的订单~
        if (!Objects.equals(orderInfo.getCustomerId(), customerId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        OrderInfoVo orderInfoVo = orderInfoConvert.toOrderInfoVo(orderInfo);
        orderInfoVo.setOrderId(orderId);

        // 1.设置司机的基本信息
        if (orderInfo.getDriverId() != null) {
            DriverInfoVo driverInfoVo =
                    driverInfoFeignClient.getDriverInfoVo(orderInfo.getDriverId())
                            .throwOnFailureOrDataIsNull()
                            .getData();
            orderInfoVo.setDriverInfoVo(driverInfoVo);
        }

        // 2.账单的信息
        boolean hasBill = OrderStatus.hasBill(orderInfo.getStatus());
        if (hasBill) {
            OrderBillVo orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
            orderInfoVo.setOrderBillVo(orderBillVo);
        }


        return orderInfoVo;
    }

    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        OrderInfo orderInfo = orderInfoResult.throwOnFailureOrDataIsNull().getData();
        if (!Objects.equals(orderInfo.getCustomerId(), customerId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<DriverInfoVo> driverInfoVo = driverInfoFeignClient.getDriverInfoVo(orderInfo.getDriverId());
        return driverInfoVo.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public OrderLocationVo getCacheOrderLocation(Long customerId, Long orderId) {
        Result<Boolean> orderInfoResult = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        orderInfoResult.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(orderInfoResult.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<OrderLocationVo> cacheOrderLocation = locationFeignClient.getCacheOrderLocation(orderId);
        return cacheOrderLocation.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public DrivingLineVo calculateDrivingLine(CalculateDrivingLineForm calculateDrivingLineForm) {
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        return drivingLineVoResult.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long customerId, Long orderId) {
        // 可以再加个状态校验
        Result<Boolean> result = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        result.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(result.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<OrderServiceLastLocationVo> orderServiceLastLocation =
                locationFeignClient.getOrderServiceLastLocation(orderId);
        return orderServiceLastLocation.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public PageVo<OrderListVo> findCustomerOrderPage(Long customerId, Long page, Long limit) {
        Result<PageVo<OrderListVo>> customerOrderPage = orderInfoFeignClient.findCustomerOrderPage(customerId, page,
                limit);
        customerOrderPage.throwOnFailureOrDataIsNull();
        return customerOrderPage.getData();
    }


    @Override
    public WxPrepayVo createWxPayment(CreateWxPaymentForm createWxPaymentForm) {
        // 1.获取订单支付相关信息
        CompletableFuture<OrderPayVo> orderPayVoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            OrderPayVo orderPayVo = orderInfoFeignClient
                    .getOrderPayVo(createWxPaymentForm.getOrderNo(), createWxPaymentForm.getCustomerId())
                    .throwOnFailureOrDataIsNull()
                    .getData();
            // 判断是否在未支付状态
            if (!Objects.equals(orderPayVo.getStatus(), OrderStatus.UNPAID.getStatus())) {
                throw new GuiguException(ResultCodeEnum.REPEAT_SUBMIT);
            }
            return orderPayVo;
        }, sharedThreadPool);
        // 2.获取乘客微信openId
        CompletableFuture<String> customerOpenIdFuture =
                orderPayVoCompletableFuture.thenApplyAsync(orderPayVo -> customerInfoFeignClient
                        .getCustomerOpenId(orderPayVo.getCustomerId())
                        .throwOnFailureOrDataIsNull()
                        .getData(), sharedThreadPool);


        // 3.获取乘客微信openId
        CompletableFuture<String> DriverOpenIdFuture =
                orderPayVoCompletableFuture.thenApplyAsync((orderPayVo) ->
                        driverInfoFeignClient
                                .getDriverOpenId(orderPayVo.getDriverId())
                                .throwOnFailureOrDataIsNull()
                                .getData(), sharedThreadPool);

        // 4.获取并更新优惠券的信息
        CompletableFuture<BigDecimal> couponAmountCompletableFuture =
                orderPayVoCompletableFuture.thenApplyAsync(orderPayVo ->
                {
                    // 支付时选择过一次优惠券，如果支付失败或未支付，下次支付时不能再次选择，只能使用第一次选中的优惠券
                    // （前端已控制，后端再次校验）
                    BigDecimal couponAmount = null;
                    if (null == orderPayVo.getCouponAmount() &&
                            null != createWxPaymentForm.getCustomerCouponId() &&
                            createWxPaymentForm.getCustomerCouponId() != 0) {
                        UseCouponForm useCouponForm = new UseCouponForm();
                        useCouponForm.setOrderId(orderPayVo.getOrderId());
                        useCouponForm.setCustomerCouponId(createWxPaymentForm.getCustomerCouponId());
                        useCouponForm.setOrderAmount(orderPayVo.getPayAmount());
                        useCouponForm.setCustomerId(createWxPaymentForm.getCustomerId());
                        couponAmount =
                                couponFeignClient.useCoupon(useCouponForm).throwOnFailureOrDataIsNull().getData();
                    }
                    return couponAmount;
                }, sharedThreadPool);


        // 5.假如使用了优惠券,更新账单支付信息
        CompletableFuture<BigDecimal> billUpdateCompletableFuture =
                orderPayVoCompletableFuture.thenCombineAsync(couponAmountCompletableFuture, (orderPayVo,
                                                                                             couponAmount) -> {
                    BigDecimal payAmount = orderPayVo.getPayAmount();
                    if (couponAmount != null) {
                        Boolean isUpdate = orderInfoFeignClient.updateCouponAmount(orderPayVo.getOrderId(),
                                couponAmount).getData();
                        if (!isUpdate) {
                            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
                        }
                        // 当前支付金额 = 支付金额 - 优惠券金额
                        payAmount = orderPayVo.getPayAmount().subtract(couponAmount);

                    }
                    return payAmount;
                }, sharedThreadPool);

        // 合并所有结果
        CompletableFuture<WxPrepayVo> resultFuture = CompletableFuture.allOf(orderPayVoCompletableFuture,
                customerOpenIdFuture, DriverOpenIdFuture, couponAmountCompletableFuture, billUpdateCompletableFuture).thenApplyAsync(v -> {
            // 4.封装微信下单对象，微信支付只关注以下订单属性
            PaymentInfoForm paymentInfoForm = new PaymentInfoForm();
            String customerOpenId = customerOpenIdFuture.join();
            String driverOpenId = DriverOpenIdFuture.join();
            OrderPayVo orderPayVo = orderPayVoCompletableFuture.join();
            BigDecimal payAmount = billUpdateCompletableFuture.join();
            paymentInfoForm.setCustomerOpenId(customerOpenId);
            paymentInfoForm.setDriverOpenId(driverOpenId);
            paymentInfoForm.setOrderNo(orderPayVo.getOrderNo());
            paymentInfoForm.setAmount(payAmount);
            paymentInfoForm.setContent(orderPayVo.getContent());
            paymentInfoForm.setPayWay(PayType.WECHAT_PAY.getType());
            return wxPayFeignClient.createWxPayment(paymentInfoForm).throwOnFailureOrDataIsNull().getData();
        }, sharedThreadPool);
        try {
            return resultFuture.get(5, TimeUnit.SECONDS); // 设置总超时
        } catch (TimeoutException e) {
            log.warn("订单={}创建微信支付超时", createWxPaymentForm.getOrderNo());
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            log.warn("订单id={}结束服务异常={}", createWxPaymentForm.getOrderNo(), e);
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }

    }

    @Override
    public Boolean queryPayStatus(String orderNo) {
        return wxPayFeignClient.queryPayStatus(orderNo).throwOnFailureOrDataIsNull().getData();
    }


}
