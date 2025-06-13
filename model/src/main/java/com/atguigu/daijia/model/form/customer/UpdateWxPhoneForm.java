package com.atguigu.daijia.model.form.customer;

import com.atguigu.daijia.model.validate.group.ServiceGroup;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.groups.Default;
import lombok.Data;

@Data
public class UpdateWxPhoneForm {

    @Schema(description = "客户Id")
    @NotBlank(groups = {ServiceGroup.class})
    private Long customerId;

    @NotBlank(groups = {ServiceGroup.class, Default.class})
    private String code;

}
