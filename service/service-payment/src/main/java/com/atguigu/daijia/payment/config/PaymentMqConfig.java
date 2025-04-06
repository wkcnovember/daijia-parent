package com.atguigu.daijia.payment.config;

import com.atguigu.daijia.common.constant.MqConst;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 柯佳元
 * @Create 2025/4/6 16:59
 * @Version 1.0
 * Description:
 */
@Configuration
public class PaymentMqConfig {

    @Bean
    public DirectExchange directExchange(){
        //默认就是持久化的
        return ExchangeBuilder.directExchange(MqConst.EXCHANGE_ORDER).build();
    }

    @Bean
    public Queue queue(){
        //队列持久化
        return QueueBuilder.durable(MqConst.QUEUE_PAY_SUCCESS).build();
    }

    @Bean
    public Binding binding(DirectExchange directExchange, Queue queue){
        return BindingBuilder.bind(queue).to(directExchange).with(MqConst.ROUTING_PAY_SUCCESS);
    }


}
