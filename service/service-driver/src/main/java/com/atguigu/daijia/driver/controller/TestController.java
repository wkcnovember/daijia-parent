package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Author 柯佳元
 * @Create 2025/3/22 16:58
 * @Version 1.0
 * Description:
 */

@Slf4j
@Tag(name = "测试1")
@RestController
@RequestMapping(value = "/test")
public class TestController {

    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;

    @Resource
    private CiService ciService;


    @Operation(summary = "test")
    @GetMapping("/t2")
    public Result<CustomerLoginVo> t2(@RequestPart("file") MultipartFile file) {
        Result<CustomerLoginVo> customerInfo = customerInfoFeignClient.getCustomerInfo(1L);
        return customerInfo;
    }


    @Operation(summary = "上传")
    @GetMapping("/t1")
    public Result<CustomerLoginVo> upload() {
        Result<CustomerLoginVo> customerInfo = customerInfoFeignClient.getCustomerInfo(1L);
        return customerInfo;
    }
}
