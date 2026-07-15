package com.wyc.wyc_rabbitmq.consumer;

import com.alibaba.fastjson2.JSON;
import com.rabbitmq.client.Channel;
import com.wyc.wyc_rabbitmq.constant.QueueConstant;
import com.wyc.wyc_rabbitmq.entity.Order;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OrderConsumer {
    // 本地缓存模拟消费记录表，生产环境用数据库唯一索引实现幂等
    private static final Set<String> consumeRecord = ConcurrentHashMap.newKeySet();

    @RabbitListener(queues = QueueConstant.QUEUE_ORDER_CONSUME)
    public void consume(String msgBody,
                        Channel channel,
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag,
                        @Header(AmqpHeaders.MESSAGE_ID) String msgId) throws Exception {

        // 1.幂等判断：重复消息直接返回，不执行业务
        if (consumeRecord.contains(msgId)) {
            System.out.println("重复消息，直接跳过 msgId=" + msgId);
            channel.basicAck(tag, false);
            return;
        }

        try {
            // 执行业务逻辑
            Order order = JSON.parseObject(msgBody, Order.class);
            System.out.println("下游消费订单：" + order.getOrderNo());

            // 记录已消费ID
            consumeRecord.add(msgId);
            // 手动确认消息
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // 消费异常，拒绝消息，重回队列重试
            channel.basicNack(tag, false, true);
            // 确认成功，删除消息，第二个参数multiple=false单条确认
//            channel.basicAck(deliveryTag, false);
            // 消费失败，消息重回队列
//            channel.basicNack(tag, false, true);
            // 拒绝消息，不重回队列（配合死信交换机使用）
//            channel.basicReject(tag, false);
        }
    }
}
