package com.atguigu.daijia.model.form.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateWxPhoneForm {

	@Schema(description = "客户Id")
	@NotBlank
	private Long customerId;

	@NotBlank
	private String code;

}
