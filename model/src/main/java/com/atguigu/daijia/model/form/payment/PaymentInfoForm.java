package com.atguigu.daijia.model.form.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.math.BigDecimal;

@Data
public class PaymentInfoForm {

    @Schema(description = "乘客微信openid")
    @NotBlank
    private String customerOpenId;

    @Schema(description = "司机微信openid")
    @NotBlank
    private String driverOpenId;

    @Schema(description = "订单号")
    @NotBlank
    private String orderNo;

    @Schema(description = "付款方式：1-微信;2-支付宝")
    @NotNull
    @Range(min = 0,max = 1)
    private Integer payWay;

    @Schema(description = "支付金额")
    @NotNull
    @Positive
    private BigDecimal amount;

    @Schema(description = "交易内容")
    @NotBlank
    private String content;

}
