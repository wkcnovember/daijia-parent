package com.atguigu.daijia.model.enums.payment;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/4/5 21:17
 * @Version 1.0
 * Description:
 */
@Getter
public enum PayStatus {

    UN_PAID(0, "未支付"), PAID(1, "已支付");

    private final Integer status;
    private final String comment;

    PayStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }


}
