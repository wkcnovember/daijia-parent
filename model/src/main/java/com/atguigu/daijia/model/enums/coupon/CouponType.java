package com.atguigu.daijia.model.enums.coupon;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/4/9 10:05
 * @Version 1.0
 * Description:
 */
@Getter
public enum CouponType {
    CASH_COUPON(1, "现金券"),
    DISCOUNT_COUPON(2, "折扣");
    private final Integer type;
    private final String comment;

    CouponType(Integer status, String comment) {
        this.type = status;
        this.comment = comment;
    }
}
