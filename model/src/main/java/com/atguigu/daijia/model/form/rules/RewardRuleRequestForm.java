package com.atguigu.daijia.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RewardRuleRequestForm {

    @Schema(description = "代驾时间")
    @NotNull
    private LocalDateTime startTime;

    @Schema(description = "订单个数")
    @NotNull
    @Positive
    private Long orderNum;

}
