package com.atguigu.daijia.coupon.listener;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.dto.coupon.CouponInfoDto;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author 柯佳元
 * @Create 2025/4/11 18:55
 * @Version 1.0
 * Description: 抢到优惠券更新优惠券领取和用户信息
 */
@Component
@Slf4j
public class CouponReceiver {

    @Resource
    private CouponInfoService couponInfoService;



    @RabbitListener(queues = {MqConst.QUEUE_COUPON_SUCCESS})
    public void couponSuccess(Message message, Channel channel) throws IOException {
        //获取消息的唯一标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String couponInfoDtoJson = new String(message.getBody());
            CouponInfoDto couponInfoDto = JSON.parseObject(couponInfoDtoJson, CouponInfoDto.class);
            couponInfoService.handleCouponInfoDto(couponInfoDto);
            //手动确认
            channel.basicAck(deliveryTag,false);
        } catch (Exception e) {
            log.error("消息处理出现问题");
            channel.basicNack(deliveryTag,false,false);

        }


    }
}
