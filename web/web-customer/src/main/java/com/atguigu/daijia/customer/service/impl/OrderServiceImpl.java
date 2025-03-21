package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.OrderService;
import com.atguigu.daijia.map.client.MapFeignClient;
import com.atguigu.daijia.model.convert.map.CalculateDrivingLineConvert;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.rules.FeeRuleRequestForm;
import com.atguigu.daijia.model.vo.customer.ExpectOrderVo;
import com.atguigu.daijia.model.vo.map.DrivingLineVo;
import com.atguigu.daijia.model.vo.rules.FeeRuleResponseVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.rules.client.FeeRuleFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;

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

    @Override
    public ExpectOrderVo expectOrder(ExpectOrderForm expectOrderForm) {



        CalculateDrivingLineForm calculateDrivingLineForm =
                calculateDrivingLineConvert.toCalculateDrivingLine(expectOrderForm);
        Result<DrivingLineVo> drivingLineVoResult = mapFeignClient.calculateDrivingLine(calculateDrivingLineForm);
        drivingLineVoResult.throwOnFailure();

        ExpectOrderVo expectOrderVo = new ExpectOrderVo();
        DrivingLineVo drivingLineVo = drivingLineVoResult.getData();
        expectOrderVo.setDrivingLineVo(drivingLineVo);

        FeeRuleRequestForm feeRuleRequestForm = new FeeRuleRequestForm();
        feeRuleRequestForm.setDistance(drivingLineVo.getDistance());
        feeRuleRequestForm.setStartTime(LocalTime.now());
        feeRuleRequestForm.setWaitMinute(0);
        Result<FeeRuleResponseVo> feeRuleResponseVoResult = feeRuleFeignClient.calculateOrderFee(feeRuleRequestForm);
        feeRuleResponseVoResult.throwOnFailure();
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
        longResult.throwOnFailure();
        Long orderId = longResult.getData();
        // TODO 查询附近可以接单司机
        return orderId;
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        return null;
    }
}
