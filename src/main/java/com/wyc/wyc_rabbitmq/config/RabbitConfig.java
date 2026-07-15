package com.wyc.wyc_rabbitmq.config;

import com.wyc.wyc_rabbitmq.constant.QueueConstant;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    // 订单直连交换机
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(QueueConstant.ORDER_EXCHANGE, true, false);
    }

    // 订单消费队列
    @Bean
    public Queue orderQueue() {
        return new Queue(QueueConstant.QUEUE_ORDER_CONSUME, true);
    }

    // 绑定交换机与队列
    @Bean
    public Binding orderBinding(DirectExchange orderExchange, Queue orderQueue) {
        return BindingBuilder.bind(orderQueue)
                .to(orderExchange)
                .with(QueueConstant.ROUTING_KEY_ORDER_CREATE);
    }
}
