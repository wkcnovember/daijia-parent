package com.atguigu.daijia.model.form.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UseCouponForm {

    @Schema(description = "乘客id")
    @NotNull
    @Positive
    private Long customerId;

    @Schema(description = "乘客优惠券id")
    @NotNull
    @Positive
    private Long customerCouponId;

    @Schema(description = "订单id")
    @NotNull
    @Positive
    private Long orderId;

    @Schema(description = "订单金额")
    @NotNull
    @Positive
    private BigDecimal orderAmount;

}
