package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
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


    /**
     * 获取乘客在进行的订单
     *
     * @param customerId
     * @return
     */
    @GetMapping("/searchCustomerCurrentOrder/{customerId}")
    Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable("customerId") Long customerId);

    /**
     * 司机端查找正在进行的订单
     *
     * @param driverId
     * @return
     */
    @GetMapping("/searchDriverCurrentOrder/{driverId}")
    Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable("driverId") Long driverId);

    /**
     * 根据订单id获取当前订单信息
     *
     * @param orderId
     * @return
     */
    @GetMapping("/getOrderInfo/{orderId}")
    Result<OrderInfo> getOrderInfo(@PathVariable("orderId") Long orderId);

}
