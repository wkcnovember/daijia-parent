package com.atguigu.daijia.payment.listener;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.model.form.payment.ProfitsharingForm;
import com.atguigu.daijia.payment.service.WxPayService;
import com.atguigu.daijia.payment.service.WxProfitsharingService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @Author 柯佳元
 * @Create 2025/4/6 16:40
 * @Version 1.0
 * Description:支付成功后的方法-接收端
 */
@Component
@Slf4j
public class PaymentReceiver {

    @Resource
    private WxPayService wxPayService;

    @Resource
    private WxProfitsharingService wxProfitsharingService;

    // @Bean
    // public RetryOperationsInterceptor retryInterceptor() {
    //     return RetryInterceptorBuilder.stateless()
    //             .maxAttempts(3 + 1) // +1 for initial attempt
    //             .backOffOptions(1000, 2.0, 5000) // 初始间隔，倍数，最大间隔
    //             .recoverer(new RejectAndDontRequeueRecoverer()) // 达到最大重试后拒绝
    //             .build();
    // }

    @RabbitListener(queues = {MqConst.QUEUE_PAY_SUCCESS})
    public void paySuccess(Message message, Channel channel) throws IOException {
        //获取消息的唯一标识
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            String orderNo = new String(message.getBody());
            wxPayService.handleOrder(orderNo);
            //手动确认
            channel.basicAck(deliveryTag,false);
        } catch (Exception e) {
            log.error("消息处理出现问题");
            channel.basicNack(deliveryTag,false,false);

        }


    }

    /**
     * 分账消息
     * @param
     * @throws IOException
     */
    @RabbitListener(queues = MqConst.QUEUE_PROFITSHARING)
    public void profitSharingMessage(Message message, Channel channel) throws IOException {
        try {

            ProfitsharingForm profitsharingForm = JSON.parseObject(new String(message.getBody()), ProfitsharingForm.class);
            log.info("分账：{}", profitsharingForm);
            wxProfitsharingService.profitsharing(profitsharingForm);
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            log.info("分账调用失败：{}", e.getMessage());
            //任务执行失败，就退回队列继续执行，优化：设置退回次数，超过次数记录日志
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
