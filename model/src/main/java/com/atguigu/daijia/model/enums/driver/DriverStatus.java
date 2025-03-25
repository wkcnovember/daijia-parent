package com.atguigu.daijia.model.enums.driver;

import lombok.Getter;

/**
 * @Author 柯佳元
 * @Create 2025/3/25 17:30
 * @Version 1.0
 * Description:
 */
@Getter
public enum DriverStatus {
    UNVERIFIED(0, "未认证"),
    PENDING(1, "审核中"),
    VERIFIED(2, "认证通过"),
    REJECTED(-1, "认证未通过");

    private final Integer code;
    private final String description;

    DriverStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }
}
