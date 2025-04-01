package com.atguigu.daijia.order.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.StartDriveForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.form.order.UpdateOrderCartForm;
import com.atguigu.daijia.model.query.order.OrderCount;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderListVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.validation.annotation.Validated;
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


    /**
     * 司机到达起始点
     *
     * @param orderId
     * @param driverId
     * @return
     */
    @GetMapping("/driverArriveStartLocation/{orderId}/{driverId}")
    Result<Boolean> driverArriveStartLocation(@PathVariable("orderId") Long orderId,
                                              @PathVariable("driverId") Long driverId);

    /**
     * 更新代驾车辆信息
     *
     * @param updateOrderCartForm
     * @return
     */
    @PostMapping("/updateOrderCart")
    Result<Boolean> updateOrderCart(@RequestBody UpdateOrderCartForm updateOrderCartForm);

    /**
     * 校验顾客订单合法性
     *
     * @param customerId
     * @param orderId
     * @return
     */
    @GetMapping("/isCustomerCurrentOrder/{customerId}/{orderId}")
    Result<Boolean> isCustomerCurrentOrder(@PathVariable("customerId") Long customerId,
                                           @PathVariable("orderId") Long orderId
    );

    /**
     * 校验司机订单合法性
     *
     * @param driverId
     * @param orderId
     * @return
     */
    @GetMapping("/isDriverCurrentOrder/{driverId}/{orderId}")
    Result<Boolean> isDriverOrder(@PathVariable("driverId") Long driverId,
                                  @PathVariable("orderId") Long orderId
    );

    /**
     * 开始代驾服务
     *
     * @param startDriveForm
     * @return
     */
    @PostMapping("/startDrive")
    Result<Boolean> startDrive(@RequestBody StartDriveForm startDriveForm);


    /**
     * 根据时间段获取订单数
     *
     * @param orderCount
     * @return
     */
    @GetMapping("/getOrderNumByTime")
    Result<Long> getOrderNumByTime(@RequestBody OrderCount orderCount);


    /**
     * 结束代驾服务更新订单账单
     *
     * @param updateOrderBillForm
     * @return
     */
    @PostMapping("/endDrive")
    Result<Boolean> endDrive(@RequestBody @Validated UpdateOrderBillForm updateOrderBillForm);


    /**
     * 获取乘客订单分页列表
     *
     * @param customerId
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/findCustomerOrderPage/{customerId}/{page}/{limit}")
    Result<PageVo<OrderListVo>> findCustomerOrderPage(@PathVariable("customerId") Long customerId,
                                                      @PathVariable("page") Long page,
                                                      @PathVariable("limit") Long limit);

    /**
     * 获取司机订单分页列表
     *
     * @param driverId
     * @param page
     * @param limit
     * @return
     */
    @GetMapping("/findDriverOrderPage/{driverId}/{page}/{limit}")
    Result<PageVo<OrderListVo>> findDriverOrderPage(
            @PathVariable("driverId") Long driverId,
            @PathVariable("page") Long page,
            @PathVariable("limit") Long limit);


}
