package com.atguigu.daijia.customer.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-customer", path = "/customer/info")
public interface CustomerInfoFeignClient {


    @GetMapping("/login/{code}")
    Result<Long> login(@PathVariable String code);

    @GetMapping("/getCustomerInfo/{customerId}")
    Result<CustomerLoginVo> getCustomerInfo(@PathVariable("customerId") Long customerId);

    @PostMapping("/updateWxPhoneNumber")
    Result<Boolean> updateWxPhoneNumber(@RequestBody UpdateWxPhoneForm updateWxPhoneForm);

}
