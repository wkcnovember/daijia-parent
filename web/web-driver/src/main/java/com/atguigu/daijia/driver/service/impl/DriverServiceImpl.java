package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.dispatch.client.NewOrderFeignClient;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.map.client.LocationFeignClient;
import com.atguigu.daijia.model.constants.redis.AuthConstents;
import com.atguigu.daijia.model.enums.driver.DriverStatus;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class DriverServiceImpl implements DriverService {


    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private LocationFeignClient locationFeignClient;
    @Resource
    private NewOrderFeignClient newOrderFeignClient;

    @Override
    public String login(String code) {
        Result<Long> longResult = driverInfoFeignClient.login(code);
        if (!ResultCodeEnum.SUCCESS.getCode().equals(longResult.getCode()))
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        Long driverId = longResult.getData();
        if (driverId == null) throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);

        // token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 放到redis，设置过期时间
        stringRedisTemplate.opsForValue().set(AuthConstents.DRIVER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                AuthConstents.DRIVER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);
        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {

        Result<DriverLoginVo> loginVoResult = driverInfoFeignClient.getDriverInfo(driverId);
        loginVoResult.throwOnFailure();
        return loginVoResult.getData();
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {

        Result<Boolean> result = driverInfoFeignClient.updateDriverAuthInfo(updateDriverAuthInfoForm);
        result.throwOnFailure();
        return result.getData();
    }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {

        Result<DriverAuthInfoVo> authInfoVoResult = driverInfoFeignClient.getDriverAuthInfo(driverId);
        DriverAuthInfoVo driverAuthInfoVo = authInfoVoResult.getData();
        return driverAuthInfoVo;
    }

    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> booleanResult = driverInfoFeignClient.creatDriverFaceModel(driverFaceModelForm);
        booleanResult.throwOnFailure();
        return booleanResult.getData();
    }

    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        Result<Boolean> result = driverInfoFeignClient.verifyDriverFace(driverFaceModelForm);
        result.throwOnFailure();
        return result.getData();
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        Result<Boolean> faceRecognition = driverInfoFeignClient.isFaceRecognition(driverId);
        faceRecognition.throwOnFailure();
        return faceRecognition.getData();
    }

    @Override
    public Boolean startService(Long driverId) {
        // 1.司机是否认证通过?
        Result<DriverLoginVo> driverLoginVoResult = driverInfoFeignClient.getDriverInfo(driverId);
        driverLoginVoResult.throwOnFailureAndDataIsNull();
        DriverLoginVo driverLoginVo = driverLoginVoResult.getData();
        if (!Objects.equals(DriverStatus.VERIFIED.getCode(), driverLoginVo.getAuthStatus())) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 2.当日是否人脸识别~
        Result<Boolean> faceRecognitionRes = driverInfoFeignClient.isFaceRecognition(driverId);
        faceRecognitionRes.throwOnFailureAndDataIsNull();
        Boolean isFace = faceRecognitionRes.getData();
        if (Boolean.FALSE.equals(faceRecognitionRes.getData())) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 3.更新司机订单状态
        Result<Boolean> result = driverInfoFeignClient.updateServiceStatus(driverId,
                DriverConstant.ServiceStatus.ACCEPTING_ORDERS.getStatus());
        result.throwOnFailureAndDataIsNull();

        Boolean updateRes = result.getData();
        if (Boolean.FALSE.equals(updateRes)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 4 删除redis司机位置信息
        locationFeignClient.removeDriverLocation(driverId);
        // 5 清空司机临时队列数据
        newOrderFeignClient.clearNewOrderQueueData(driverId);
        return Boolean.TRUE;

    }

    @Override
    public Boolean stopService(Long driverId) {
        //更新司机的接单状态 0
        Result<Boolean> result = driverInfoFeignClient.updateServiceStatus(driverId,
                DriverConstant.ServiceStatus.NOT_ACCEPTED_ORDERS.getStatus());
        result.throwOnFailureAndDataIsNull();
        //删除司机位置信息
        Result<Boolean> result1 = locationFeignClient.removeDriverLocation(driverId);
        result1.throwOnFailureAndDataIsNull();
        //清空司机临时队列
        Result<Boolean> result2 = newOrderFeignClient.clearNewOrderQueueData(driverId);
        result2.throwOnFailureAndDataIsNull();
        return Boolean.TRUE;
    }
}
