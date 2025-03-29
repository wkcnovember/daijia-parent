package com.atguigu.daijia.model.form.order;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "OrderInfo")
public class StartDriveForm {


    @Schema(description = "订单ID")
    @NotNull
    @Positive
	private Long orderId;

    @Schema(description = "司机ID")
    @NotNull(groups = {ServiceGroup.class})
    @Positive(groups = {ServiceGroup.class})
    private Long driverId;

}
