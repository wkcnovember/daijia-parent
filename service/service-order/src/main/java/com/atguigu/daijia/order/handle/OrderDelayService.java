package com.atguigu.daijia.order.handle;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.order.service.OrderInfoService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class OrderDelayService {

    private final RBlockingQueue<String> orderQueue;
    private final RDelayedQueue<String> delayedQueue;

    private final   OrderInfoService orderInfoService;


    @Autowired
    public OrderDelayService(RedissonClient redissonClient,@Lazy OrderInfoService orderInfoService) {
        // 1 创建队列
        this.orderQueue = redissonClient.getBlockingQueue(RedisConstant.ORDER_BLOCKED_QUEUE);
        // 2 把创建队列放到延迟队列里面
        this.delayedQueue = redissonClient.getDelayedQueue(orderQueue);

        this.orderInfoService = orderInfoService;

    }

    /**
     * 添加订单到延迟队列，15分钟后处理
     */
    public void addOrderToDelayQueue(String orderId) {
        delayedQueue.offer(orderId, RedisConstant.ORDER_BLOCKED_QUEUE_TIMEOUT, TimeUnit.MINUTES);
    }

    /**
     * 启动延迟队列监听
     */
    @PostConstruct
    public void startDelayQueueConsumer() {
        new Thread(() -> {
            while (true) {
                try {
                    // 1.从延迟队列获取过期的订单
                    String orderId = orderQueue.take();
                    if (StringUtils.isNotBlank(orderId)) {
                        // 处理超时订单
                      orderInfoService.orderCancel(Long.parseLong(orderId));
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }




    /**
     * 应用关闭时销毁队列
     */
    @PreDestroy
    public void destroy() {
        delayedQueue.destroy();
    }
}
