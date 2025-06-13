package com.atguigu.daijia.model.vo.dispatch;

import com.atguigu.daijia.model.validate.map.anno.ValidLatitude;
import com.atguigu.daijia.model.validate.map.anno.ValidLongitude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class NewOrderTaskVo {

	@Schema(description = "订单id")
	@NotNull
	private Long orderId;

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

	@Schema(description = "结束地点维度")
	@ValidLatitude
	private BigDecimal endPointLatitude;

	@Schema(description = "预估订单金额")
	@NotNull
	private BigDecimal expectAmount;

	@Schema(description = "预估里程")
	@NotNull
	private BigDecimal expectDistance;

	@Schema(description = "预估时间")
	@NotNull
	private BigDecimal expectTime;

	@Schema(description = "顾客好处费")
	@NotNull
	private BigDecimal favourFee;

	@Schema(description = "下单时间")
	@NotNull
	private LocalDateTime createTime;
}
