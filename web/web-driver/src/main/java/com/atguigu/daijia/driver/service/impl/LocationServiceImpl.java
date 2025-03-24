package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.LocationService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
public class LocationServiceImpl implements LocationService {

    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;


    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {

        // 根据司机id获取司机个性化设置信息
        Result<DriverSetVo> result = driverInfoFeignClient.getDriverSet(updateDriverLocationForm.getDriverId());
        result.throwOnFailure();
        DriverSetVo driverSetVo = result.getData();

        // 判断：如果司机开始接单，更新位置信息
        //  接单的话,维护每个司机的订单队列,默认没有接到订单的话15分钟过期
        Integer serviceStatus = driverSetVo.getServiceStatus();
        if(Objects.equals(serviceStatus, DriverConstant.ServiceStatus.ACCEPTING_ORDERS.getStatus())) {
            Result<Boolean> locationRes = locationFeignClient.updateDriverLocation(updateDriverLocationForm);
            locationRes.throwOnFailure();
            return locationRes.getData();
        }else {
            //没有接单
            throw new GuiguException(ResultCodeEnum.NO_START_SERVICE);
        }

    }
}
