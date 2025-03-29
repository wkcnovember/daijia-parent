package com.atguigu.daijia.model.form.order;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "更新订单车辆表单")
public class UpdateOrderCartForm {

    @NotNull(message = "订单ID不能为空")
    @Schema(description = "订单ID")
    private Long orderId;

    @Schema(description = "司机ID")
    // @NotNull(message = "订单ID不能为空",groups = {ServiceGroup.class})
    private Long driverId;

    @NotBlank(message = "车牌号不能为空")
    @Schema(description = "车牌号")
    private String carLicense;

    @Schema(description = "车型")
    private String carType;

    @NotBlank(message = "车前照不能为空")
    @Schema(description = "司机到达拍照：车前照")
    private String carFrontUrl;

    @NotBlank(message = "车后照不能为空")
    @Schema(description = "司机到达拍照：车后照")
    private String carBackUrl;


}
