package com.atguigu.daijia.model.form.map;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateDriverLocationForm {

    @Schema(description = "司机id")
    @NotNull(groups = {ServiceGroup.class})
    @Positive(groups = {ServiceGroup.class})
    private Long driverId;

    @Schema(description = "经度")
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(description = "维度")
    @ValidLatitude
    private BigDecimal latitude;

}
