package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.convert.map.CalculateDrivingLineConvert;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Date;
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
        feeRuleRequestForm.setStartTime(LocalTime.now());
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
        feeRuleRequestForm.setStartTime(LocalTime.now());
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
        },sharedThreadPool);


        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        orderStatus.throwOnFailure();
        return orderStatus.getData();
    }

    @Override
    public Boolean customerCancelNoAcceptOrder(Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.updateOrderStatus(orderId,
                OrderStatus.CUSTOMER_CANCEL_ORDER.getStatus());
        result.throwOnFailureOrDataIsNull();
        return result.getData();
    }
}
