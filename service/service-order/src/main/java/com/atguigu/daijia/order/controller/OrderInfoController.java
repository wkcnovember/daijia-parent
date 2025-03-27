package com.atguigu.daijia.order.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.order.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


@Tag(name = "订单API接口管理")
@RestController
@RequestMapping(value = "/order/info")
@Validated
public class OrderInfoController {
    @Resource
    private OrderInfoService orderInfoService;

    @Operation(summary = "保存订单信息")
    @PostMapping("/saveOrderInfo")
    public Result<Long> saveOrderInfo(@RequestBody @Validated OrderInfoForm orderInfoForm) {
        return Result.ok(orderInfoService.saveOrderInfo(orderInfoForm));
    }

    @Operation(summary = "根据订单id获取订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.getOrderStatus(orderId));
    }



    @Operation(summary = "修改订单状态")
    @PutMapping("/updateOrderStatus/{orderId}/{status}")
    public Result<Boolean> updateOrderStatus(@PathVariable("orderId") @NotNull @Positive Long orderId,
                                       @PathVariable("status") @NotNull Integer status) {
        return Result.ok(orderInfoService.updateOrderStatus(orderId,status));
    }

    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                       @PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.robNewOrder(driverId, orderId));
    }


}

