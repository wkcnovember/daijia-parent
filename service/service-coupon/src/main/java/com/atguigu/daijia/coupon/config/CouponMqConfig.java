package com.atguigu.daijia.coupon.config;

import com.atguigu.daijia.common.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 柯佳元
 * @Create 2025/4/11 18:44
 * @Version 1.0
 * Description:
 */
@Configuration
public class CouponMqConfig {

    @Bean
    public DirectExchange directCouponExchange(){
        //默认就是持久化的
        return ExchangeBuilder.directExchange(MqConst.EXCHANGE_COUPON).build();
    }

    @Bean
    public Queue couponQueue(){
        //队列持久化
        return QueueBuilder.durable(MqConst.QUEUE_COUPON_SUCCESS).build();
    }


    @Bean
    public Binding binding(DirectExchange directCouponExchange, Queue couponQueue){
        return BindingBuilder.bind(couponQueue).to(directCouponExchange).with(MqConst.ROUTING_COUPON_SUCCESS);
    }

}
