package com.atguigu.daijia.model.form.rules;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;

@Data
public class FeeRuleRequestForm {

    @Schema(description = "代驾里程")
    @NotNull
    private BigDecimal distance;

    @Schema(description = "代驾时间")
    @NotNull
    private LocalTime startTime;

    @Schema(description = "等候分钟")
    @NotNull
    private Integer waitMinute;

}
