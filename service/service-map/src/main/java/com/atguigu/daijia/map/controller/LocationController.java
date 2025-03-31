package com.atguigu.daijia.map.controller;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.validate.group.ServiceGroup;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.groups.Default;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Tag(name = "位置API接口管理")
@RestController
@RequestMapping("/map/location")
@Validated
public class LocationController {

    @Resource
    private LocationService locationService;

    // 司机开启接单，更新司机位置信息
    @Operation(summary = "开启接单服务：更新司机经纬度位置")
    @PostMapping("/updateDriverLocation")
    public Result<Boolean> updateDriverLocation(@RequestBody
                                                @Validated({ServiceGroup.class, Default.class})
                                                UpdateDriverLocationForm updateDriverLocationForm) {
        Boolean flag = locationService.updateDriverLocation(updateDriverLocationForm);
        return Result.ok(flag);
    }

    // 司机关闭接单，删除司机位置信息,可以是微信小程序被关闭发送一个请求~
    @Operation(summary = "删除司机经纬度位置")
    @DeleteMapping("/removeDriverLocation/{driverId}")
    public Result<Boolean> removeDriverLocation(@PathVariable @NotNull @Positive Long driverId) {
        return Result.ok(locationService.removeDriverLocation(driverId));
    }

    @Operation(summary = "搜索附近满足条件的司机")
    @PostMapping("/searchNearByDriver")
    public Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody
                                                           @Validated
                                                           SearchNearByDriverForm searchNearByDriverForm) {
        return Result.ok(locationService.searchNearByDriver(searchNearByDriverForm));
    }

    @Operation(summary = "司机赶往代驾起始点：更新订单地址到缓存")
    @PostMapping("/updateOrderLocationToCache")
    public Result<Boolean> updateOrderLocationToCache(@RequestBody @Validated UpdateOrderLocationForm updateOrderLocationForm) {
        return Result.ok(locationService.updateOrderLocationToCache(updateOrderLocationForm));
    }

    @Operation(summary = "司机赶往代驾起始点：获取司机当前经纬度位置")
    @GetMapping("/getCacheOrderLocation/{orderId}")
    public Result<OrderLocationVo> getCacheOrderLocation(@PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(locationService.getCacheOrderLocation(orderId));
    }

    @Operation(summary = "开始代驾服务：批量保存代驾服务订单位置")
    @PostMapping("/saveOrderServiceLocation")
    public Result<Boolean> saveOrderServiceLocation(@RequestBody @Validated @NotEmpty List<OrderServiceLocationForm> orderLocationServiceFormList) {
        return Result.ok(locationService.saveOrderServiceLocation(orderLocationServiceFormList));
    }

    @Operation(summary = "代驾服务：获取订单服务最后一个(最新)位置信息")
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    public Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable @NotNull @Positive Long orderId) {
        return Result.ok(locationService.getOrderServiceLastLocation(orderId));
    }

    @Operation(summary = "代驾服务：计算订单实际里程")
    @GetMapping("/calculateOrderRealDistance/{orderId}")
    public Result<BigDecimal> calculateOrderRealDistance(@PathVariable("orderId") @NotNull @Positive Long orderId) {
        return Result.ok(locationService.calculateOrderRealDistance(orderId));
    }

}

