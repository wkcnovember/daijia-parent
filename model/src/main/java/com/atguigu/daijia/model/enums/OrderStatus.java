package com.atguigu.daijia.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum OrderStatus {
    WAITING_ACCEPT(1, "等待接单"),
    ACCEPTED(2, "已接单"),
    DRIVER_ARRIVED(3, "司机已到达"),
    START_SERVICE(4, "开始代驾"),
    END_SERVICE(5, "结束代驾"),
    UNPAID(6, "待付款"),
    PAID(7, "已付款"),
    FINISH(8, "订单已完成"),
    CUSTOMER_CANCEL_ORDER(9, "顾客撤单"),
    DRIVER_CANCEL_ORDER(10, "司机撤单"),
    ACCIDENT_CLOSE(11, "事故关闭"),
    ORDER_TIMEOUT(12, "超时取消订单"),
    CANCEL_ORDER(-1, "未接单取消订单"),
    NULL_ORDER(-100, "不存在"),
    ;

    @EnumValue
    private Integer status;
    private String comment;

    OrderStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
