package com.atguigu.daijia.model.constants.redis;

/**
 * @Author 柯佳元
 * @Create 2025/3/24 16:54
 * @Version 1.0
 * Description:
 */
public class AuthConstents {
    public static final String CUSTOMER_LOGIN_KEY_PREFIX = "customer:login:";
    public static final String DRIVER_LOGIN_KEY_PREFIX = "driver:login:";
    public static final String MANAGER_LOGIN_KEY_PREFIX = "manager:login:";

    public static final int CUSTOMER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
    public static final int DRIVER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
    public static final int MANAGER_LOGIN_KEY_TIMEOUT = 60 * 60 * 24 * 100;
}
