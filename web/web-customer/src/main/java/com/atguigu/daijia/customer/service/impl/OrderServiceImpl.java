package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.convert.map.CalculateDrivingLineConvert;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.order.OrderStatus;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderBillVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderListVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
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

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {


        // 预估驾驶路线
        CalculateDrivingLineForm calculateDrivingLineForm =
                calculateDrivingLineConvert.toCalculateDrivingLine(expectOrderForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        drivingLineVoResult.throwOnFailureOrDataIsNull();

        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
        expectOrderVo.setDrivingLineVo(drivingLineVo);

        // 预估订单金额

        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(LocalDateTime.now());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        feeRuleResponseVoResult.throwOnFailureOrDataIsNull();
        FeeRuleResponseVo data = feeRuleResponseVoResult.getData();
        expectOrderVo.setFeeRuleResponseVo(data);

        return expectOrderVo;
    }

    @Override
    public Long submitOrder(SubmitOrderForm submitOrderForm) {

        // 1 重新计算驾驶线路
        CalculateDrivingLineForm calculateDrivingLineForm =
                calculateDrivingLineConvert.toCalculateDrivingLineBySb(submitOrderForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        drivingLineVoResult.throwOnFailure();


        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
        // 2 重新订单费用
        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        BigDecimal distance = drivingLineVo.getDistance();
        feeRuleRequestForm.setDistance(distance);
        feeRuleRequestForm.setStartTime(LocalDateTime.now());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        feeRuleResponseVoResult.throwOnFailure();
        FeeRuleResponseVo feeRuleResponseVo = feeRuleResponseVoResult.getData();

        // 3.封装数据订单
        OrderInfoForm orderInfoForm = orderInfoConvert.toOrderInfoForm(submitOrderForm);
        orderInfoForm.setExpectAmount(feeRuleResponseVo.getTotalAmount());
        orderInfoForm.setExpectDistance(distance);

        // 4.远程调用订单添加接口~
        Result<Long> longResult = orderInfoFeignClient.saveOrderInfo(orderInfoForm);
        longResult.throwOnFailureOrDataIsNull();
        Long orderId = longResult.getData();


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
            newOrderDispatchVo.setCreateTime(new Date());
            // 远程调用
            newOrderFeignClient.addAndStartTask(newOrderDispatchVo);
            // Long jobId = newOrderFeignClient.addAndStartTask(newOrderDispatchVo).getData();
        }, sharedThreadPool);


        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        Result<Boolean> result = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        result.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(result.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        orderStatus.throwOnFailure();
        return orderStatus.getData();
    }

    @Override
    public Boolean customerCancelNoAcceptOrder(Long orderId) {
        Long customerId = AuthContextHolder.getUserId();
        Result<Boolean> isValidateResult = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        isValidateResult.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(isValidateResult.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Boolean> result = orderInfoFeignClient.updateOrderStatus(orderId,
                OrderStatus.CUSTOMER_CANCEL_ORDER.getStatus());
        result.throwOnFailureOrDataIsNull();
        return result.getData();
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
        if (orderInfo.getStatus() >= OrderStatus.UNPAID.getStatus()) {
            OrderBillVo orderBillVo = orderInfoFeignClient.getOrderBillInfo(orderId).getData();
            orderInfoVo.setOrderBillVo(orderBillVo);
        }


        return orderInfoVo;
    }

    @Override
    public DriverInfoVo getDriverInfo(Long orderId, Long customerId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        orderInfoResult.throwOnFailureOrDataIsNull();
        OrderInfo orderInfo = orderInfoResult.getData();
        if (!Objects.equals(orderInfo.getCustomerId(), customerId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<DriverInfoVo> driverInfoVo = driverInfoFeignClient.getDriverInfoVo(orderInfo.getDriverId());
        driverInfoVo.throwOnFailureOrDataIsNull();
        return driverInfoVo.getData();
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
        drivingLineVoResult.throwOnFailureOrDataIsNull();
        return drivingLineVoResult.getData();
    }

    @Override
    public OrderServiceLastLocationVo getOrderServiceLastLocation(Long customerId, Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.isCustomerCurrentOrder(customerId, orderId);
        result.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(result.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<OrderServiceLastLocationVo> orderServiceLastLocation =
                locationFeignClient.getOrderServiceLastLocation(orderId);
        orderServiceLastLocation.throwOnFailureOrDataIsNull();
        return orderServiceLastLocation.getData();
    }

    @Override
    public PageVo<OrderListVo> findCustomerOrderPage(Long customerId, Long page, Long limit) {
        Result<PageVo<OrderListVo>> customerOrderPage = orderInfoFeignClient.findCustomerOrderPage(customerId, page,
                limit);
        customerOrderPage.throwOnFailureOrDataIsNull();
        return customerOrderPage.getData();
    }


}
