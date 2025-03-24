package com.atguigu.daijia.dispatch.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.vo.dispatch.NewOrderTaskVo;
import com.atguigu.daijia.model.vo.order.NewOrderDataVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


@FeignClient(value = "service-dispatch", path = "/dispatch/newOrder")
public interface NewOrderFeignClient {

    @PostMapping("/addAndStartTask")
    Result<Long> addAndStartTask(@RequestBody @Validated NewOrderTaskVo newOrderTaskVo);

    @GetMapping("/findNewOrderQueueData/{driverId}")
    Result<List<NewOrderDataVo>> findNewOrderQueueData(@PathVariable("driverId")  Long driverId);

    @GetMapping("/clearNewOrderQueueData/{driverId}")
    Result<Boolean> clearNewOrderQueueData(@PathVariable("driverId") Long driverId);


}
