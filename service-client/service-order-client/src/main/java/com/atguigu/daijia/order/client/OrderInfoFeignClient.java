package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(value = "service-order", path = "/order/info")
public interface OrderInfoFeignClient {

    /**
     * 保存订单信息
     *
     * @param orderInfoForm
     * @return
     */
    @PostMapping("/saveOrderInfo")
    Result<Long> saveOrderInfo(@RequestBody OrderInfoForm orderInfoForm);

    @GetMapping("/getOrderStatus/{orderId}")
    Result<Integer> getOrderStatus(@PathVariable Long orderId);

    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    Result<Boolean> robNewOrder(@PathVariable("driverId") Long driverId,
                                @PathVariable("orderId") Long orderId);


    /**
     * 修改订单状态
     *
     * @param orderId
     * @param status
     * @return
     */
    @PutMapping("/updateOrderStatus/{orderId}/{status}")
    Result<Boolean> updateOrderStatus(@PathVariable("orderId") Long orderId,
                                      @PathVariable("status") Integer status);

}
