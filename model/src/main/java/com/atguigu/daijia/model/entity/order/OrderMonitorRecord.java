package com.atguigu.daijia.model.entity.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Schema(description = "OrderMonitorRecord")
@Document("order_monitor_record")
public class OrderMonitorRecord  {

	@Schema(description = "id")
	@Id
	private ObjectId id;

	@Schema(description = "订单ID")
	@NotNull
	@Positive
	private Long orderId;

	@Schema(description = "文件路径")
	@NotBlank
	private String fileUrl;

	@Schema(description = "内容")
	private String content;

	@Schema(description = "审核结果")
	private String result;

	@Schema(description = "风险关键词")
	private String keywords;

	@Schema(description = "状态")
	private Integer status;

}
