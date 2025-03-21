package com.atguigu.daijia.customer.controller;

import com.atguigu.daijia.common.auth.KjyLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "客户API接口管理")
@RestController
@RequestMapping("/customer")
@Validated
public class CustomerController {


    @Resource
    private CustomerService customerService;

    @Operation(summary = "小程序授权登录")
    @GetMapping("/login/{code}")
    public Result<String> wxLogin(@PathVariable @NotBlank  String code) {
        return Result.ok(customerService.login(code));
    }


    @Operation(summary = "获取客户登录信息")
    @KjyLogin
    @GetMapping("/getCustomerLoginInfo")
    public Result<CustomerLoginVo> getCustomerLoginInfo() {
        Long userId = AuthContextHolder.getUserId();
        CustomerLoginVo customerLoginVo = customerService.getCustomerLoginInfo(userId);
        return Result.ok(customerLoginVo);
    }

    @Operation(summary = "更新用户微信手机号")
    @KjyLogin
    @PostMapping("/updateWxPhone")
    public Result updateWxPhone(@RequestBody @Validated UpdateWxPhoneForm updateWxPhoneForm) {
        updateWxPhoneForm.setCustomerId(AuthContextHolder.getUserId());
        return Result.ok(customerService.updateWxPhoneNumber(updateWxPhoneForm));
    }

    // @Operation(summary = "获取客户登录信息")
    // @GetMapping("/getCustomerLoginInfo")
    // public Result<CustomerLoginVo> getCustomerLoginInfo(@RequestHeader(value = "token") String token) {
    //     CustomerLoginVo customerLoginVo = customerService.getCustomerLoginInfo(token);
    //     return Result.ok(customerLoginVo);
    // }


}

