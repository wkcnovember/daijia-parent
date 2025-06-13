package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerInfoVo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.tencentcloudapi.ccc.v20200210.models.ServeParticipant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/customer/info")
@Tag(name = "客户接口")
@Validated
public class CustomerInfoController {

    @Resource
    private CustomerInfoService customerInfoService;

    @Operation(summary = "更新客户微信手机号码")
    @PostMapping("/updateWxPhoneNumber")
    public Result<Boolean> updateWxPhoneNumber(@RequestBody @Validated(value = {ServeParticipant.class}) UpdateWxPhoneForm updateWxPhoneForm) {
        return Result.ok(customerInfoService.updateWxPhoneNumber(updateWxPhoneForm));
    }


    @Operation(summary = "获取客户基本信息")
    @GetMapping("/getCustomerInfo/{customerId}")
    public Result<CustomerLoginVo> getCustomerInfo(@PathVariable("customerId") @NotNull Long customerId) {
        CustomerLoginVo customerLoginVo = customerInfoService.getCustomerInfo(customerId);
        return Result.ok(customerLoginVo);
    }

    // 微信小程序登录接口
    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<Long> login(@PathVariable @NotBlank String code) {
        return Result.ok(customerInfoService.login(code));
    }


    @Operation(summary = "获取客户信息(司机显示)")
    @GetMapping("/getCustomerInfoVo/{customerId}")
    public Result<CustomerInfoVo> getCustomerInfoVo(@PathVariable("customerId") @NotNull @Positive Long customerId) {
        CustomerInfoVo customerLoginVo = customerInfoService.getCustomerInfoVo(customerId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "获取客户OpenId")
    @GetMapping("/getCustomerOpenId/{customerId}")
    public Result<String> getCustomerOpenId(@PathVariable("customerId") @NotNull @Positive Long customerId) {
        return Result.ok(customerInfoService.getCustomerOpenId(customerId));
    }
}

