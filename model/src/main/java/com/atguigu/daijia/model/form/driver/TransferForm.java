package com.atguigu.daijia.model.form.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransferForm {

    @Schema(description = "司机id")
    @NotNull
    @Positive
    private Long driverId;

    @Schema(description = "交易内容")
    @NotBlank
    private String content;

    @Schema(description = "交易类型")
    @NotNull
    private Integer tradeType;

    @Schema(description = "交易金额")
    @NotNull
    @Positive
    private BigDecimal amount;

    @Schema(description = "交易编号")
    @NotBlank
    private String tradeNo;

}
