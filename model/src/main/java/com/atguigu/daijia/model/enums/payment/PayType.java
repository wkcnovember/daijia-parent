package com.atguigu.daijia.model.enums.payment;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/4/5 21:33
 * @Version 1.0
 * Description:
 */
@Getter
public enum PayType {

    WECHAT_PAY(1, "微信支付"), ALI_PAY(2, "支付宝支付");

    private final Integer type;
    private final String comment;

    PayType(Integer type, String comment) {
        this.type = type;
        this.comment = comment;
    }
}
