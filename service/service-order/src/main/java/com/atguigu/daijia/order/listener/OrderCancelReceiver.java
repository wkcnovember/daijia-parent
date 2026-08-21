package com.atguigu.daijia.order.listener;

import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 支付超时关单消息消费者
 *
 * 监听死信队列（订单支付超时 15 分钟后由延迟队列转入）。
 * 拿到订单 id 后交给 orderTimeoutCancel 处理，该方法内部会校验订单状态，保证幂等。
 */
@Component
@Slf4j
public class OrderCancelReceiver {

    @Resource
    private OrderInfoService orderInfoService;

    @RabbitListener(queues = {MqConst.QUEUE_CANCEL_ORDER})
    public void orderTimeoutCancel(Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String orderIdStr = new String(message.getBody());
            Long orderId = Long.parseLong(orderIdStr);
            orderInfoService.orderTimeoutCancel(orderId);
            // 手动确认：消费成功
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("支付超时关单处理异常，orderId={}", new String(message.getBody()), e);
            // 处理失败：不重回队列，避免死循环（后续可接入日志告警）
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
