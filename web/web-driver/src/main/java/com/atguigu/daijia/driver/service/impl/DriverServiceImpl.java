package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.DriverInfoFeignClient;
import com.atguigu.daijia.driver.service.DriverService;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverServiceImpl implements DriverService {


    @Resource
    private DriverInfoFeignClient driverInfoFeignClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String login(String code) {
        Result<Long> longResult = driverInfoFeignClient.login(code);
        if (!ResultCodeEnum.SUCCESS.getCode().equals(longResult.getCode()))
            throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);
        Long driverId = longResult.getData();
        if (Objects.isNull(driverId)) throw new GuiguException(ResultCodeEnum.LOGIN_ERROR);

        // token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");
        // 放到redis，设置过期时间
        stringRedisTemplate.opsForValue().set(RedisConstant.USER_LOGIN_KEY_PREFIX + token,
                driverId.toString(),
                RedisConstant.USER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);
        return token;
    }

    @Override
    public DriverLoginVo getDriverLoginInfo(Long driverId) {
        Result<DriverLoginVo> loginVoResult = driverInfoFeignClient.getDriverInfo(driverId);
        if (!ResultCodeEnum.SUCCESS.getCode().equals(loginVoResult.getCode()) ||
                Objects.isNull(loginVoResult.getData())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return loginVoResult.getData();
    }
}
