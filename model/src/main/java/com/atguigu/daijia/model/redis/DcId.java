package com.atguigu.daijia.model.redis;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * @Author 柯佳元
 * @Create 2025/3/29 22:10
 * @Version 1.0
 * Description:
 */
@Schema(name = "司机与乘客的id")
@Data
public class DcId {
    private Long driverId;
    private Long customerId;

}
