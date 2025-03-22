package com.atguigu.daijia.model.form.map;

import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class SearchNearByDriverForm {

    @Schema(description = "经度")
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(description = "维度")
    @ValidLatitude
    private BigDecimal latitude;

    @Schema(description = "里程")
    @NotNull
    private BigDecimal mileageDistance;
}
