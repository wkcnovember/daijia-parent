package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
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
import java.util.concurrent.*;

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


    @Resource
    private ThreadPoolExecutor sharedThreadPool;

    @Override
    public String login(String code) {
        Result<Long> longResult = driverInfoFeignClient.login(code);
        Long driverId = longResult.throwOnFailureOrDataIsNull().getData();

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
        return loginVoResult.throwOnFailureOrDataIsNull().getData();
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
        result.throwOnFailureOrDataIsNull();
        return result.getData();
    }

    @Override
    public Boolean isFaceRecognition(Long driverId) {
        Result<Boolean> faceRecognition = driverInfoFeignClient.isFaceRecognition(driverId);
        return faceRecognition.throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public Boolean startService(Long driverId) {
        // 1.司机是否认证通过?
        Result<DriverLoginVo> driverLoginVoResult = driverInfoFeignClient.getDriverInfo(driverId);
        DriverLoginVo driverLoginVo = driverLoginVoResult.throwOnFailureOrDataIsNull().getData();
        if (!Objects.equals(DriverStatus.VERIFIED.getCode(), driverLoginVo.getAuthStatus())) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 2.当日是否人脸识别~
        Result<Boolean> faceRecognitionRes = driverInfoFeignClient.isFaceRecognition(driverId);
        Boolean isFace = faceRecognitionRes.throwOnFailureOrDataIsNull().getData();
        if (Boolean.FALSE.equals(isFace)) {
            throw new GuiguException(ResultCodeEnum.AUTH_ERROR);
        }
        // 3.更新司机订单状态
        Result<Boolean> result = driverInfoFeignClient.updateServiceStatus(driverId,
                DriverConstant.ServiceStatus.ACCEPTING_ORDERS.getStatus());
        Boolean updateRes = result.throwOnFailureOrDataIsNull().getData();
        if (Boolean.FALSE.equals(updateRes)) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 3. 并行清理旧数据
        CompletableFuture<Void> locationFuture = CompletableFuture.runAsync(() -> {
            locationFeignClient.removeDriverLocation(driverId);
        }, sharedThreadPool);

        CompletableFuture<Void> orderQueueFuture = CompletableFuture.runAsync(() -> {
            newOrderFeignClient.clearNewOrderQueueData(driverId);
        }, sharedThreadPool);

        // 等待清理完成
        try {
            CompletableFuture.allOf(locationFuture, orderQueueFuture).get(5, TimeUnit.SECONDS);
            return Boolean.TRUE;
        } catch (TimeoutException e) {
            log.warn("司机={}开启服务清理旧地址队列数据超时", driverId);
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            log.warn("司机={}开启服务清理旧地址队列数据异常={}", driverId, e);
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }


    }

    @Override
    public Boolean stopService(Long driverId) {
        CompletableFuture<Void> updateDriverStatusFuture = CompletableFuture.runAsync(() -> {
            // 更新司机的接单状态 0
            Result<Boolean> result = driverInfoFeignClient.updateServiceStatus(driverId,
                    DriverConstant.ServiceStatus.NOT_ACCEPTED_ORDERS.getStatus());
            result.throwOnFailureOrDataIsNull();
        }, sharedThreadPool);


        CompletableFuture<Void> clearLocationFuture = CompletableFuture.runAsync(() -> {
            // 删除司机位置信息
            Result<Boolean> result1 = locationFeignClient.removeDriverLocation(driverId);
            result1.throwOnFailureOrDataIsNull();
        }, sharedThreadPool);

        CompletableFuture<Void> clearOrderQueue = CompletableFuture.runAsync(() -> {
            // 清空司机订单临时队列
            Result<Boolean> result2 = newOrderFeignClient.clearNewOrderQueueData(driverId);
            result2.throwOnFailureOrDataIsNull();
        }, sharedThreadPool);

        try {
            CompletableFuture.allOf(updateDriverStatusFuture, clearLocationFuture, clearOrderQueue)
                    .get(5, TimeUnit.SECONDS);
            return Boolean.TRUE;
        } catch (TimeoutException e) {
            log.warn("司机={}停止服务超时", driverId);
            throw new GuiguException(ResultCodeEnum.REMOTE_TIMEOUT);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            log.warn("司机={}停止服务异常={}", driverId, e);
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Task interrupted", e); // 记录中断日志
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }


    }
}
