package com.atguigu.daijia.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProfitsharingRuleRequestForm {

    @Schema(description = "订单金额")
    @NotNull
    @Positive
    private BigDecimal orderAmount;

    @Schema(description = "当天完成订单个数")
    @NotNull
    @Positive
    private Long orderNum;

}
