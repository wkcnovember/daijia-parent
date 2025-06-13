package com.atguigu.daijia.order.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.enums.order.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.query.order.OrderCount;
import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.*;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;


@Tag(name = "订单API接口管理")
@RestController
@RequestMapping(value = "/order/info")
@Validated
public class OrderInfoController {
    @Resource
    private OrderInfoService orderInfoService;

    @Operation(summary = "保存订单信息")
    @PostMapping("/saveOrderInfo")
    public Result<Long> saveOrderInfo(@RequestBody @Validated({ServiceGroup.class, Default.class}) OrderInfoForm orderInfoForm) {
        return Result.ok(orderInfoService.saveOrderInfo(orderInfoForm));
    }

    @Operation(summary = "根据订单id获取订单状态")
    @GetMapping("/getOrderStatus/{orderId}")
    public Result<Integer> getOrderStatus(@PathVariable @NotNull @Positive Long orderId) {
        OrderInfo orderInfo = orderInfoService.getOne(new LambdaQueryWrapper<OrderInfo>()
                .select(OrderInfo::getStatus)
                .eq(BaseEntity::getId, orderId)
        );
        return Result.ok(orderInfo == null ? OrderStatus.NULL_ORDER.getStatus() : orderInfo.getStatus());
    }

    @Operation(summary = "根据用户订单id获取订单状态")
    @GetMapping("/getCustomerOrderStatus/{customerId}/{orderId}")
    public Result<Integer> getCustomerOrderStatus(
            @PathVariable("customerId") @NotNull @Positive Long customerId,
            @PathVariable("orderId") @NotNull @Positive Long orderId
    ) {
        Integer orderStatus = orderInfoService.getOrderStatus(orderId, customerId, false);
        return Result.ok(orderStatus);
    }

    @Operation(summary = "根据司机订单id获取订单状态")
    @GetMapping("/getDriverOrderStatus/{driverId}/{orderId}")
    public Result<Integer> getDriverOrderStatus(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                                @PathVariable("orderId") @NotNull @Positive Long orderId
    ) {

        Integer orderStatus = orderInfoService.getOrderStatus(orderId, driverId, true);
        return Result.ok(orderStatus);
    }


    @Operation(summary = "修改订单状态")
    @PutMapping("/updateOrderStatus/{orderId}/{status}")
    public Result<Boolean> updateOrderStatus(@PathVariable("orderId") @NotNull @Positive Long orderId,
                                             @PathVariable("status") @NotNull Integer status) {
        return Result.ok(orderInfoService.updateOrderStatus(orderId, status));
    }


    @Operation(summary = "乘客取消下单")
    @PutMapping("/customerCancelNoAcceptOrder/{customerId}/{orderId}")
    public Result<Boolean> customerCancelNoAcceptOrder(@PathVariable("customerId") @NotNull @Positive Long customerId,
                                                       @PathVariable("orderId") @NotNull @Positive Long orderId
    ) {
        return Result.ok(orderInfoService.customerCancelNoAcceptOrder(customerId, orderId));
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
    public Result<Boolean> updateOrderCart(@RequestBody @Validated({ServiceGroup.class, Default.class}) UpdateOrderCartForm updateOrderCartForm) {
        return Result.ok(orderInfoService.updateOrderCart(updateOrderCartForm));
    }

    @Operation(summary = "开始代驾服务")
    @PostMapping("/startDrive")
    public Result<Boolean> startDrive(@RequestBody @Validated({ServiceGroup.class, Default.class}) StartDriveForm startDriveForm) {
        return Result.ok(orderInfoService.startDrive(startDriveForm));
    }

    @Operation(summary = "是否处于开始服务状态")
    @PostMapping("/isStartDrive/{driverId}/{orderId}")
    public Result<Boolean> isStartDrive(@PathVariable("driverId") @NotNull @Positive Long driverId,
                                        @PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.isStartDrive(driverId, orderId));
    }

    @Operation(summary = "根据时间段获取订单数")
    @GetMapping("/getOrderNumByTime")
    public Result<Long> getOrderNumByTime(@RequestBody @Validated OrderCount orderCount) {
        return Result.ok(orderInfoService.getOrderNumByTime(orderCount));
    }

    @Operation(summary = "结束代驾服务更新订单账单")
    @PostMapping("/endDrive")
    public Result<Boolean> endDrive(@RequestBody @Validated UpdateOrderBillForm updateOrderBillForm) {
        return Result.ok(orderInfoService.endDrive(updateOrderBillForm));
    }

    @Operation(summary = "获取乘客订单分页列表")
    @GetMapping("/findCustomerOrderPage/{customerId}/{page}/{limit}")
    public Result<PageVo<OrderListVo>> findCustomerOrderPage(@PathVariable("customerId") @NotNull @Positive Long customerId,
                                                             @PathVariable("page") @NotNull @Positive Long page,
                                                             @PathVariable("limit") @NotNull @Positive Long limit) {
        // 创建page对象
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        // 调用service方法实现分页条件查询
        PageVo<OrderListVo> pageVo = orderInfoService.findCustomerOrderPage(pageParam, customerId);
        return Result.ok(pageVo);
    }

    @Operation(summary = "获取司机订单分页列表")
    @GetMapping("/findDriverOrderPage/{driverId}/{page}/{limit}")
    public Result<PageVo<OrderListVo>> findDriverOrderPage(
            @Parameter(name = "driverId", description = "司机id", required = true)
            @PathVariable("driverId") @NotNull @Positive Long driverId,
            @Parameter(name = "page", description = "当前页码", required = true)
            @PathVariable("page") @NotNull @Positive Long page,
            @Parameter(name = "limit", description = "每页记录数", required = true)
            @PathVariable("limit") @NotNull @Positive Long limit) {
        Page<OrderInfo> pageParam = new Page<>(page, limit);
        PageVo<OrderListVo> pageVo = orderInfoService.findDriverOrderPage(pageParam, driverId);
        return Result.ok(pageVo);
    }

    @Operation(summary = "根据订单id获取实际账单信息")
    @GetMapping("/getOrderBillInfo/{orderId}")
    public Result<OrderBillVo> getOrderBillInfo(@PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.getOrderBillInfo(orderId));
    }

    @Operation(summary = "根据订单id获取实际分账信息")
    @GetMapping("/getOrderProfitSharing/{orderId}")
    public Result<OrderProfitsharingVo> getOrderProfitSharing(@PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(orderInfoService.getOrderProfitSharing(orderId));
    }

    @Operation(summary = "发送账单信息")
    @GetMapping("/sendOrderBillInfo/{orderId}/{driverId}")
    Result<Boolean> sendOrderBillInfo(@PathVariable("orderId") @NotNull @Positive Long orderId, @PathVariable(
            "driverId") @NotNull @Positive Long driverId) {
        return Result.ok(orderInfoService.sendOrderBillInfo(orderId, driverId));
    }

    @Operation(summary = "获取订单支付信息")
    @GetMapping("/getOrderPayVo/{orderNo}/{customerId}")
    public Result<OrderPayVo> getOrderPayVo(@PathVariable("orderNo") @NotBlank String orderNo,
                                            @PathVariable("customerId") @NotNull @Positive Long customerId) {
        return Result.ok(orderInfoService.getOrderPayVo(orderNo, customerId));
    }

    @Operation(summary = "更改订单支付状态")
    @GetMapping("/updateOrderPayStatus/{orderNo}")
    public Result<Boolean> updateOrderPayStatus(@PathVariable("orderNo") @NotBlank String orderNo) {
        return Result.ok(orderInfoService.updateOrderPayStatus(orderNo));
    }

    @Operation(summary = "获取订单的系统奖励")
    @GetMapping("/getOrderRewardFee/{orderNo}")
    public Result<OrderRewardVo> getOrderRewardFee(@PathVariable("orderNo") @NotBlank String orderNo) {
        return Result.ok(orderInfoService.getOrderRewardFee(orderNo));
    }

    @Operation(summary = "更新订单优惠券金额")
    @GetMapping("/updateCouponAmount/{orderId}/{couponAmount}")
    public Result<Boolean> updateCouponAmount(@PathVariable("orderId") @NotNull @Positive Long orderId,
                                              @PathVariable("couponAmount") @NotNull @Positive BigDecimal couponAmount) {
        return Result.ok(orderInfoService.updateCouponAmount(orderId, couponAmount));
    }

}

