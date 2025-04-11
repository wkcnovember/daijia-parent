package com.atguigu.daijia.model.enums.coupon;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/4/11 18:52
 * @Version 1.0
 * Description:
 */
@Getter
public enum CustomerCouponStatus {
    unused(0, "未使用"),
    USED(1, "已使用");
    private final Integer status;
    private final String comment;

    CustomerCouponStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }
}
