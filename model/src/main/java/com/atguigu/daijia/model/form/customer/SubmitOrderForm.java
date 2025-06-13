package com.atguigu.daijia.model.form.customer;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SubmitOrderForm {

    @Schema(description = "乘客id")
    @NotNull(groups = {ServiceGroup.class})
    @Positive(groups = {ServiceGroup.class})
    private Long customerId;

    @Schema(description = "起始地点")
    @NotBlank
    private String startLocation;

    @Schema(description = "起始地点经度")
    @ValidLongitude
    private BigDecimal startPointLongitude;

    @Schema(description = "起始点维度")
    @ValidLatitude
    private BigDecimal startPointLatitude;

    @Schema(description = "结束地点")
    @NotBlank
    private String endLocation;

    @Schema(description = "结束地点经度")
    @ValidLongitude
    private BigDecimal endPointLongitude;

    @Schema(description = "结束地点经度")
    @ValidLatitude
    private BigDecimal endPointLatitude;

    @Schema(description = "顾客好处费")
    private BigDecimal favourFee = BigDecimal.ZERO;

}
