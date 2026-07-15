package com.wyc.wyc_rabbitmq.util;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * convertAndSend 同步阻塞，线程等待 MQ 服务接收消息才会返回
 */
@Component
public class MqSendUtil {

    @Resource
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送MQ消息（同步阻塞执行）
     */
    public void sendMsg(String exchange, String routingKey, String data, String msgId) {
        rabbitTemplate.convertAndSend(exchange, routingKey, data, message -> {
            // 消息唯一ID，下游消费幂等使用
            message.getMessageProperties().setMessageId(msgId);
            return message;
        });
        System.out.println("MQ投递完成 msgId=" + msgId);
    }
}
