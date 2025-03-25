package com.atguigu.daijia.model.form.driver;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DriverFaceModelForm {

    @Schema(description = "司机id")
    @NotNull(groups = ServiceGroup.class)
    private Long driverId;

    @Schema(description = "图片 base64 数据")
    @NotBlank
    private String imageBase64 ;
}
