package com.atguigu.daijia.payment.service.impl;

import com.atguigu.daijia.payment.service.MockPay;
import com.atguigu.daijia.payment.service.WxPayService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @Author 柯佳元
 * @Create 2025/4/5 22:29
 * @Version 1.0
 * Description: 模拟支付成功
 */
@Service
@Slf4j
public class MockPayImpl implements MockPay {


    @Resource
    private WxPayService wxPayService;

    @Async
    @Override
    public Boolean wxNotify(String orderNo) {
        try {
            TimeUnit.SECONDS.sleep(2);
            log.info("微信回调==>微信支付成功!");
            TimeUnit.SECONDS.sleep(5);
            log.info("处理支付业务");
            wxPayService.handlePayment(orderNo);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        return Boolean.TRUE;
    }



}
