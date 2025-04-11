package com.atguigu.daijia.model.dto.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author 柯佳元
 * @Create 2025/4/11 18:39
 * @Version 1.0
 * Description:
 */
@Data
public class CouponInfoDto {

    @Schema(name = "优惠券id")
    private Long id;

    @Schema(name = "领取的客户id")
    private Long customerId;

    @Schema(name = "领取时间")
    private LocalDateTime receiveTime;

    @Schema(name = "优化券状态（1：未使用 2：已使用）")
    private Integer status;
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;
}
