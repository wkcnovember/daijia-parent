package com.atguigu.daijia.model.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    WAITING_ACCEPT(1, "等待接单"),
    ACCEPTED(2, "已接单"),
    DRIVER_ARRIVED(3, "司机已到达"),
    UPDATE_CART_INFO(4, "更新代驾车辆信息"),
    START_SERVICE(5, "开始代驾"),
    END_SERVICE(6, "结束代驾"),
    UNPAID(7, "待付款"),
    PAID(8, "已付款"),
    FINISH(9, "订单已完成"),
    CUSTOMER_CANCEL_ORDER(10, "顾客撤单"),
    DRIVER_CANCEL_ORDER(11, "司机撤单"),
    ACCIDENT_CLOSE(12, "事故关闭"),
    ORDER_TIMEOUT(13, "超时取消订单"),
    CANCEL_ORDER(-1, "未接单取消订单"),
    NULL_ORDER(-100, "不存在"),
    ;

    private final Integer status;
    private final String comment;

    OrderStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }


}
