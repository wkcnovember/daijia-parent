package com.atguigu.daijia.model.form.order;

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
@Schema(description = "OrderInfo")
public class OrderInfoForm {


    @Schema(description = "客户ID")
    @NotNull
	private Long customerId;

    @Schema(description = "订单号")
	private String orderNo;

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

    @Schema(description = "结束地点纬度")
    @ValidLatitude
	private BigDecimal endPointLatitude;

    @Schema(description = "顾客好处费")
    private BigDecimal favourFee;

    @Schema(description = "订单备注信息")
	private String remark;


    //预期费用信息
    @Schema(description = "预估订单费用")
    @NotNull
    private BigDecimal expectAmount;

    @Schema(description = "预估里程")
    @NotNull
    private BigDecimal expectDistance;

}
