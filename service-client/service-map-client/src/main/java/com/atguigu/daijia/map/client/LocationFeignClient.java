package com.atguigu.daijia.map.client;

import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import com.atguigu.daijia.model.vo.map.OrderLocationVo;
import com.atguigu.daijia.model.vo.map.OrderServiceLastLocationVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(value = "service-map", path = "/map/location")
public interface LocationFeignClient {

    /**
     * 开启接单服务：更新司机经纬度位置
     *
     * @param updateDriverLocationForm
     * @return
     */
    @PostMapping("/updateDriverLocation")
    Result<Boolean> updateDriverLocation(@RequestBody UpdateDriverLocationForm updateDriverLocationForm);

    /**
     * 关闭接单服务：删除司机经纬度位置
     *
     * @param driverId
     * @return
     */
    @DeleteMapping("/removeDriverLocation/{driverId}")
    Result<Boolean> removeDriverLocation(@PathVariable("driverId") Long driverId);


    @PostMapping("/searchNearByDriver")
    Result<List<NearByDriverVo>> searchNearByDriver(@RequestBody
                                                    SearchNearByDriverForm searchNearByDriverForm);

    /**
     * 司机赶往代驾起始点：更新订单地址到缓存
     *
     * @param updateOrderLocationForm
     * @return
     */
    @PostMapping("/updateOrderLocationToCache")
    Result<Boolean> updateOrderLocationToCache(@RequestBody UpdateOrderLocationForm updateOrderLocationForm);

    /**
     * 司机赶往代驾起始点：获取订单经纬度位置
     *
     * @param orderId
     * @return
     */
    @GetMapping("/getCacheOrderLocation/{orderId}")
    Result<OrderLocationVo> getCacheOrderLocation(@PathVariable("orderId") Long orderId);

    /**
     * 开始代驾服务：保存代驾服务订单位置
     *
     * @param orderLocationServiceFormList
     * @return
     */
    @PostMapping("/saveOrderServiceLocation")
    Result<Boolean> saveOrderServiceLocation(@RequestBody List<OrderServiceLocationForm> orderLocationServiceFormList);


    /**
     * 代驾服务：获取订单服务最后一个位置信息
     *
     * @param orderId
     * @return
     */
    @GetMapping("/getOrderServiceLastLocation/{orderId}")
    Result<OrderServiceLastLocationVo> getOrderServiceLastLocation(@PathVariable Long orderId);


}
