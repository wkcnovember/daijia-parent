package com.atguigu.daijia.order.config;

import com.atguigu.daijia.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 支付超时关单消息配置
 *
 * 链路说明：
 * 发账单（订单置 UNPAID）→ 投递消息到【延迟交换机】→ 进入【延迟队列】（带 TTL 15 分钟）
 * → TTL 到期后消息自动转入【死信交换机】→ 路由到【死信队列】（业务队列）
 * → 消费者监听死信队列，检查订单状态决定是否关单
 */
@Configuration
public class OrderMqConfig {

    // 1. 延迟交换机
    @Bean
    public DirectExchange cancelOrderDelayExchange() {
        return ExchangeBuilder.directExchange(MqConst.EXCHANGE_CANCEL_ORDER_DELAY).durable(true).build();
    }

    // 2. 延迟队列：设置 TTL 与死信交换机
    @Bean
    public Queue cancelOrderDelayQueue() {
        return QueueBuilder.durable(MqConst.QUEUE_CANCEL_ORDER_DELAY)
                // 消息在队列中存活 15 分钟，到期后进入死信交换机
                .ttl(MqConst.CANCEL_ORDER_DELAY_TIME)
                .deadLetterExchange(MqConst.EXCHANGE_CANCEL_ORDER_DLX)
                .deadLetterRoutingKey(MqConst.ROUTING_CANCEL_ORDER_DLX)
                .build();
    }

    // 3. 延迟队列绑定到延迟交换机
    @Bean
    public Binding cancelOrderDelayBinding() {
        return BindingBuilder.bind(cancelOrderDelayQueue())
                .to(cancelOrderDelayExchange())
                .with(MqConst.ROUTING_CANCEL_ORDER_DELAY);
    }

    // 4. 死信交换机
    @Bean
    public DirectExchange cancelOrderDlxExchange() {
        return ExchangeBuilder.directExchange(MqConst.EXCHANGE_CANCEL_ORDER_DLX).durable(true).build();
    }

    // 5. 死信队列（真正被消费者监听的业务队列）
    @Bean
    public Queue cancelOrderQueue() {
        return QueueBuilder.durable(MqConst.QUEUE_CANCEL_ORDER).build();
    }

    // 6. 死信队列绑定到死信交换机
    @Bean
    public Binding cancelOrderDlxBinding() {
        return BindingBuilder.bind(cancelOrderQueue())
                .to(cancelOrderDlxExchange())
                .with(MqConst.ROUTING_CANCEL_ORDER_DLX);
    }
}
