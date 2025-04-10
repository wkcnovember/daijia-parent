package com.atguigu.daijia.model.enums.coupon;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/4/9 10:05
 * @Version 1.0
 * Description:
 */
@Getter
public enum CouponStatus {
    UN_PUBLISH(0, "未发布"),
    PUBLISHED(1, "已发布");
    private final Integer status;
    private final String comment;

    CouponStatus(Integer status, String comment) {
        this.status = status;
        this.comment = comment;
    }
}
