package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.OrderService;
import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.vo.customer.CustomerInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class OrderServiceImpl implements OrderService {


    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;
    @Resource
    private OrderInfoConvert orderInfoConvert;
    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Override
    public Integer getOrderStatus(Long orderId) {
        Result<Integer> orderStatus = orderInfoFeignClient.getOrderStatus(orderId);
        orderStatus.throwOnFailureOrDataIsNull();
        return orderStatus.getData();
    }

    @Override
    public List<NewOrderDataVo> findNewOrderQueueData(Long driverId) {
        Result<List<NewOrderDataVo>> newOrderQueueData = newOrderFeignClient.findNewOrderQueueData(driverId);
        newOrderQueueData.throwOnFailure();
        return newOrderQueueData.getData();
    }

    @Override
    public Boolean robNewOrder(Long driverId, Long orderId) {
        Result<Boolean> result = orderInfoFeignClient.robNewOrder(driverId, orderId);
        result.throwOnFailureOrDataIsNull();
        return result.getData();
    }

    @Override
    public CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId) {
        Result<CurrentOrderInfoVo> currentOrderInfoVoResult = orderInfoFeignClient.searchDriverCurrentOrder(driverId);
        currentOrderInfoVoResult.throwOnFailureOrDataIsNull();
        return currentOrderInfoVoResult.getData();
    }

    @Override
    public OrderInfoVo getOrderInfo(Long orderId, Long driverId) {
        Result<OrderInfo> orderInfoResult = orderInfoFeignClient.getOrderInfo(orderId);
        orderInfoResult.throwOnFailureOrDataIsNull();
        OrderInfo orderInfo = orderInfoResult.getData();
        if(!Objects.equals(orderInfo.getDriverId(),driverId)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        OrderInfoVo orderInfoVo = orderInfoConvert.toOrderInfoVo(orderInfo);
        orderInfoVo.setOrderId(orderId);
        Result<CustomerInfoVo> customerInfoVoResult = customerInfoFeignClient.getCustomerInfoVo(orderInfo.getCustomerId());
        customerInfoVoResult.throwOnFailureOrDataIsNull();
        orderInfoVo.setCustomerInfoVo(customerInfoVoResult.getData());
        return orderInfoVo;
    }
}
