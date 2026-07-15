package com.wyc.wyc_rabbitmq.constant;

public class QueueConstant {
    // 订单交换机
    public static final String ORDER_EXCHANGE = "order_exchange";
    // 订单创建路由key
    public static final String ROUTING_KEY_ORDER_CREATE = "order.create";
    // 订单消费队列
    public static final String QUEUE_ORDER_CONSUME = "order_consume_queue";
}
