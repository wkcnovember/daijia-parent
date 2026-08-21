package com.atguigu.daijia.common.constant;

public class MqConst {


    // 优惠券有关的
    public static final String EXCHANGE_COUPON= "dj.cn";
    public static final String ROUTING_COUPON_SUCCESS= "dj.cn.succ";
    public static final String QUEUE_COUPON_SUCCESS = "dj.cn.queue";


    public static final String EXCHANGE_ORDER = "daijia.order";
    public static final String ROUTING_PAY_SUCCESS = "daijia.pay.success";
    public static final String ROUTING_PROFITSHARING_SUCCESS = "daijia.profitsharing.success";
    public static final String QUEUE_PAY_SUCCESS = "daijia.pay.success";
    public static final String QUEUE_PROFITSHARING_SUCCESS = "daijia.profitsharing.success";


    //取消订单延迟消息
    public static final String EXCHANGE_CANCEL_ORDER = "daijia.cancel.order";
    public static final String ROUTING_CANCEL_ORDER = "daijia.cancel.order";
    public static final String QUEUE_CANCEL_ORDER = "daijia.cancel.order";

    //支付超时关单：延迟交换机/队列（消息带 TTL，到期后转入死信交换机）
    public static final String EXCHANGE_CANCEL_ORDER_DELAY = "daijia.cancel.order.delay.exchange";
    public static final String ROUTING_CANCEL_ORDER_DELAY = "daijia.cancel.order.delay";
    public static final String QUEUE_CANCEL_ORDER_DELAY = "daijia.cancel.order.delay.queue";
    //死信交换机/路由（延迟队列消息到期后的去向）
    public static final String EXCHANGE_CANCEL_ORDER_DLX = "daijia.cancel.order.dlx";
    public static final String ROUTING_CANCEL_ORDER_DLX = "daijia.cancel.order.dead";
    //支付超时时间：15 分钟（毫秒）
    public static final int CANCEL_ORDER_DELAY_TIME = 15 * 60 * 1000;

    //分账延迟消息
    public static final String EXCHANGE_PROFITSHARING = "daijia.profitsharing";
    public static final String ROUTING_PROFITSHARING = "daijia.profitsharing";
    public static final String QUEUE_PROFITSHARING  = "daijia.profitsharing";

}
