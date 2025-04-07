package com.atguigu.daijia.order.service;

import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.query.order.OrderCount;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

public interface OrderInfoService extends IService<OrderInfo> {

    Long saveOrderInfo(OrderInfoForm orderInfoForm);

    Integer getOrderStatus(Long orderId);

    Boolean updateOrderStatus(Long orderId, Integer status);

    Boolean robNewOrder(Long driverId, Long orderId);

    CurrentOrderInfoVo searchCustomerCurrentOrder(Long customerId);

    CurrentOrderInfoVo searchDriverCurrentOrder(Long driverId);

    OrderInfo getOrderInfo(Long orderId);

    Boolean driverArriveStartLocation(Long orderId, Long driverId);

    Boolean updateOrderCart(UpdateOrderCartForm updateOrderCartForm);

    Boolean isCustomerCurrentOrder(Long customerId, Long orderId);

    Boolean isDriverCurrentOrder(Long driverId, Long orderId);

    Boolean startDrive(StartDriveForm startDriveForm);

    Long getOrderNumByTime(OrderCount orderCount);

    Boolean endDrive(UpdateOrderBillForm updateOrderBillForm);

    PageVo<OrderListVo> findCustomerOrderPage(Page<OrderInfo> pageParam, Long customerId);

    PageVo<OrderListVo> findDriverOrderPage(Page<OrderInfo> pageParam, Long driverId);

    OrderBillVo getOrderBillInfo(Long orderId);

    OrderProfitsharingVo getOrderProfitSharing(Long orderId);

    Boolean sendOrderBillInfo(Long orderId, Long driverId);

    OrderPayVo getOrderPayVo(String orderNo, Long customerId);

    Boolean updateOrderPayStatus(String orderNo);

    OrderRewardVo getOrderRewardFee(String orderNo);

    Boolean customerCancelNoAcceptOrder(Long customerId, Long orderId);

    void orderCancel(Long orderId);

    Boolean isStartDrive(Long driverId, Long orderId);
}
