package com.atguigu.daijia.model.form.map;

import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderServiceLocationForm {

    @Schema(description = "订单id")
    @NotNull
    @Positive
    private Long orderId;

    @Schema(description = "经度")
    @ValidLongitude
    private BigDecimal longitude;

    @Schema(description = "纬度")
    @ValidLatitude
    private BigDecimal latitude;

}
