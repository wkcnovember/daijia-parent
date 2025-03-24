package com.atguigu.daijia.map.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.map.service.LocationService;
import com.atguigu.daijia.model.form.map.SearchNearByDriverForm;
import com.atguigu.daijia.model.form.map.UpdateDriverLocationForm;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import com.atguigu.daijia.model.vo.map.NearByDriverVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Slf4j
@Service
public class LocationServiceImpl implements LocationService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;


    @Override
    public Boolean updateDriverLocation(UpdateDriverLocationForm updateDriverLocationForm) {
        Point point = new Point(updateDriverLocationForm.getLongitude().doubleValue(),
                updateDriverLocationForm.getLatitude().doubleValue());
        Long add = stringRedisTemplate.opsForGeo().add(RedisConstant.DRIVER_GEO_LOCATION, point,
                updateDriverLocationForm.getDriverId().toString());
        if (add == null) return Boolean.FALSE;
        return add > 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public Boolean removeDriverLocation(Long driverId) {
        stringRedisTemplate.opsForGeo().remove(RedisConstant.DRIVER_GEO_LOCATION, driverId.toString());
        return Boolean.TRUE;
    }

    @Override
    public List<NearByDriverVo> searchNearByDriver(SearchNearByDriverForm searchNearByDriverForm) {

        // 查询附件5公里滴司机~
        GeoOperations<String, String> ops = stringRedisTemplate.opsForGeo();
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                        .includeCoordinates()  // 查到详细坐标
                        .includeDistance()  //  查到距离
                        //.limit(10) // 限制10条
                        .sortAscending(); // 升序
        GeoResults<RedisGeoCommands.GeoLocation<String>> driverRes = ops.radius(RedisConstant.DRIVER_GEO_LOCATION,
                new Circle(new Point(searchNearByDriverForm.getLongitude().doubleValue(),
                        searchNearByDriverForm.getLatitude().doubleValue()),
                        new Distance(DriverConstant.NEARBY_DRIVER_RADIUS, Metrics.KILOMETERS)),
                args);
        if (null == driverRes) return Collections.emptyList();

        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> content = driverRes.getContent();


        // 根据司机个性化设计信息过滤超过接单范围的司机
        return content.stream()
                .filter(item -> {


                    String name = item.getContent().getName();
                    Long driverId = JSON.parseObject(name, Long.class);
                    Result<DriverSetVo> driverSetVoResult = driverInfoFeignClient.getDriverSet(driverId);
                    DriverSetVo driverSetVo = driverSetVoResult.getData();
                    if (driverSetVo == null) {
                        return false;
                    }


                    // 乘客距离大于司机接客距离
                    BigDecimal distance = BigDecimal.valueOf(item.getDistance().getValue());
                    BigDecimal acceptDistance = driverSetVo.getAcceptDistance();
                    if (distance.compareTo(acceptDistance) > 0) {
                        return false;
                    }

                    // 司机接单的范围
                    // 0 表示不受限制~
                    BigDecimal orderDistance = driverSetVo.getOrderDistance();
                    if (BigDecimal.ZERO.compareTo(orderDistance) == 0) {
                        return true;
                    }

                    // 司机接单的范围和乘客的距离比较 >=0 在范围内  否则不在
                    return orderDistance.compareTo(BigDecimal.valueOf(DriverConstant.NEARBY_DRIVER_RADIUS)) >= 0;
                }).map(item -> {
                    String name = item.getContent().getName();
                    Long driverId = JSON.parseObject(name, Long.class);
                    return new NearByDriverVo(driverId,
                            BigDecimal.valueOf(item.getDistance().getValue()).setScale(2, RoundingMode.HALF_UP));
                }).toList();
    }

    public static void main(String[] args) {
        BigDecimal bigDecimal1 = BigDecimal.valueOf(0.00);
        System.out.println(BigDecimal.ZERO.compareTo(bigDecimal1) == 0);
    }
}
