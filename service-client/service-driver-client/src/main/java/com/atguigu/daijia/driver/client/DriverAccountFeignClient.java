package com.atguigu.daijia.driver.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.driver.TransferForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "service-driver", path = "/driver/account")
public interface DriverAccountFeignClient {

    /**
     * 转账
     *
     * @param transferForm
     * @return
     */
    @PostMapping("/transfer")
    Result<Boolean> transfer(@RequestBody @Validated TransferForm transferForm);


}
