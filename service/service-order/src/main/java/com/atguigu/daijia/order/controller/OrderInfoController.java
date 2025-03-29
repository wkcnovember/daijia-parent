package com.atguigu.daijia.order.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.order.service.OrderInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
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
        return Result.ok(orderInfoService.updateOrderStatus(orderId, status));
    }

    @Operation(summary = "司机抢单")
    @GetMapping("/robNewOrder/{driverId}/{orderId}")
    public Result<Boolean> robNewOrder(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                       @PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.robNewOrder(driverId, orderId));
    }

    @Operation(summary = "乘客端查找当前正在进行的订单")
    @GetMapping("/searchCustomerCurrentOrder/{customerId}")
    public Result<CurrentOrderInfoVo> searchCustomerCurrentOrder(@PathVariable("customerId") @NotNull @Positive Long customerId) {
        return Result.ok(orderInfoService.searchCustomerCurrentOrder(customerId));
    }

    @Operation(summary = "司机端查找正在进行的订单")
    @GetMapping("/searchDriverCurrentOrder/{driverId}")
    public Result<CurrentOrderInfoVo> searchDriverCurrentOrder(@PathVariable("driverId") @NotNull @Positive Long driverId) {
        return Result.ok(orderInfoService.searchDriverCurrentOrder(driverId));
    }

    // todo 直接返回,后续优化~
    @Operation(summary = "根据订单id获取当前订单信息")
    @GetMapping("/getOrderInfo/{orderId}")
    public Result<OrderInfo> getOrderInfo(@PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.getOrderInfo(orderId));
    }

    //
    @Operation(summary = "校验顾客订单合法性")
    @GetMapping("/isCustomerCurrentOrder/{customerId}/{orderId}")
    public Result<Boolean> isCustomerCurrentOrder(@PathVariable("customerId") @NotNull @Positive Long customerId,
                                                  @PathVariable("orderId") @NotNull @Positive Long orderId
    ) {
        return Result.ok(orderInfoService.isCustomerCurrentOrder(customerId, orderId));
    }

    @Operation(summary = "校验司机订单合法性")
    @GetMapping("/isDriverCurrentOrder/{driverId}/{orderId}")
    public Result<Boolean> isDriverOrder(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                         @PathVariable("orderId") @NotNull @Positive Long orderId
    ) {
        return Result.ok(orderInfoService.isDriverCurrentOrder(driverId, orderId));
    }


    @Operation(summary = "司机到达起始点")
    @GetMapping("/driverArriveStartLocation/{orderId}/{driverId}")
    public Result<Boolean> driverArriveStartLocation(@PathVariable("orderId") @NotNull @Positive Long orderId,
                                                     @PathVariable("driverId") @NotNull @Positive Long driverId) {
        return Result.ok(orderInfoService.driverArriveStartLocation(orderId, driverId));
    }

    @Operation(summary = "更新代驾车辆信息")
    @PostMapping("/updateOrderCart")
    public Result<Boolean> updateOrderCart(@RequestBody @Validated UpdateOrderCartForm updateOrderCartForm) {
        return Result.ok(orderInfoService.updateOrderCart(updateOrderCartForm));
    }

    @Operation(summary = "开始代驾服务")
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody @Validated({ServiceGroup.class, Default.class}) StartDriveForm startDriveForm) {
        return Result.ok(orderInfoService.startDrive(startDriveForm));
    }


}

