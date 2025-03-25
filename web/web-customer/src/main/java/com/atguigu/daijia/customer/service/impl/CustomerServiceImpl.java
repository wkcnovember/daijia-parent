package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.client.CustomerInfoFeignClient;
import com.atguigu.daijia.customer.service.CustomerService;
import com.atguigu.daijia.model.constants.redis.AuthConstents;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerServiceImpl implements CustomerService {

    @Resource
    private CustomerInfoFeignClient customerInfoFeignClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public String login(String code) {

        // 1.通过code远程获取用户id
        Result<Long> longResult = customerInfoFeignClient.login(code);
        // 2.状态问题
        longResult.throwOnFailure();
        Long customerId = longResult.getData();
        if (null == customerId) throw new GuiguException(ResultCodeEnum.DATA_ERROR);

        // 生成token字符串
        String token = UUID.randomUUID().toString().replaceAll("-", "");

        // 6 把用户id放到Redis，设置过期时间
        // key:token  value:customerId
        stringRedisTemplate.opsForValue().set(AuthConstents.CUSTOMER_LOGIN_KEY_PREFIX + token,
                customerId.toString(),
                AuthConstents.CUSTOMER_LOGIN_KEY_TIMEOUT,
                TimeUnit.SECONDS);

        // 7 返回token
        return token;


    }

    @Override
    public CustomerLoginVo getCustomerLoginInfo(Long userId) {
        Result<CustomerLoginVo> customerLoginVoResult = customerInfoFeignClient.getCustomerInfo(userId);
        if (!customerLoginVoResult.getCode().equals(ResultCodeEnum.SUCCESS.getCode()))
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        CustomerLoginVo customerLoginVo = customerLoginVoResult.getData();
        if (null == customerLoginVo) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {
        Result<Boolean> result = customerInfoFeignClient.updateWxPhoneNumber(updateWxPhoneForm);
        if(!result.getCode().equals(ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(result.getCode(),result.getMessage());
        }
        return result.getData();
    }
}
