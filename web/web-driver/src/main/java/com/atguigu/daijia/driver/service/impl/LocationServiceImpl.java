package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.map.OrderServiceLocationForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.form.map.UpdateOrderLocationForm;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class LocationServiceImpl implements LocationService {

    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;
    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;


    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        // 根据司机id获取司机个性化设置信息
        Result<DriverSetVo> result = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId());
        result.throwOnFailure();
        DriverSetVo driverSetVo = result.getData();

        // 判断：如果司机开始接单，更新位置信息
        Integer serviceStatus = driverSetVo.getServiceStatus();
        if (Objects.equals(serviceStatus, DriverConstant.ServiceStatus.ACCEPTING_ORDERS.getStatus())) {
            Result<Boolean> locationRes = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
            locationRes.throwOnFailure();
            return locationRes.getData();
        } else {
            // 没有接单
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }

    }

    @Override
    public Boolean updateOrderLocationToCache(Long driverId, UpdateOrderLocationForm updateOrderLocationForm) {
        Result<Boolean> result = orderInfoFeignClient.isDriverOrder(driverId,
                updateOrderLocationForm.getOrderId());
        result.throwOnFailureOrDataIsNull();
        if (Boolean.FALSE.equals(result.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Result<Boolean> updateOrderLocationToCache =
                locationFeignClient.updateOrderLocationToCache(updateOrderLocationForm);
        updateOrderLocationToCache.throwOnFailureOrDataIsNull();
        return updateOrderLocationToCache.getData();
    }

    @Override
    public Boolean saveOrderServiceLocation(List<OrderServiceLocationForm> orderLocationServiceFormList) {
        if (CollectionUtils.isEmpty(orderLocationServiceFormList)) {
            return Boolean.TRUE;
        }
        Long orderId = orderLocationServiceFormList.get(0).getOrderId();
        Long driverId = AuthContextHolder.getUserId();

        Result<Boolean> isStartDriverRes = orderInfoFeignClient.isStartDrive(driverId, orderId);
        Boolean isStartDriver = isStartDriverRes.throwOnFailureOrDataIsNull().getData();
        if (Boolean.FALSE.equals(isStartDriver)) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }


        Result<Boolean> result = locationFeignClient.saveOrderServiceLocation(orderLocationServiceFormList);
        result.throwOnFailureOrDataIsNull();
        return result.getData();
    }
}
