package com.atguigu.daijia.payment.config;

import com.atguigu.daijia.common.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 柯佳元
 * @Create 2025/4/10 22:34
 * @Version 1.0
 * Description:
 */
@Configuration
public class ProfitSharingMqConfig {

    @Bean
    public DirectExchange profitSharingExchange(){
        //默认就是持久化的
        return ExchangeBuilder.directExchange(MqConst.EXCHANGE_PROFITSHARING).build();
    }

    @Bean
    public Queue profitSharingQueue(){
        //队列持久化
        return QueueBuilder.durable(MqConst.QUEUE_PROFITSHARING).build();
    }

    @Bean
    public Binding binding(DirectExchange profitSharingExchange, Queue profitSharingQueue){
        return BindingBuilder.bind(profitSharingQueue).to(profitSharingExchange).with(MqConst.ROUTING_PROFITSHARING);
    }
}
