package com.atguigu.daijia.model.form.customer;

import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ExpectOrderForm {

    @Schema(description = "起始地点经度")
    @ValidLongitude
    private BigDecimal startPointLongitude;

    @Schema(description = "起始点维度")
    @ValidLatitude
    private BigDecimal startPointLatitude;

    @Schema(description = "结束地点经度")
    @ValidLongitude
    private BigDecimal endPointLongitude;

    @Schema(description = "结束地点维度")
    @ValidLatitude
    private BigDecimal endPointLatitude;
}
