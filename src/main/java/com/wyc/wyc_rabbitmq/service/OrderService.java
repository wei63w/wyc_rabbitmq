package com.wyc.wyc_rabbitmq.service;

import com.alibaba.fastjson2.JSON;
import com.wyc.wyc_rabbitmq.constant.QueueConstant;
import com.wyc.wyc_rabbitmq.entity.Order;
import com.wyc.wyc_rabbitmq.entity.Outbox;
import com.wyc.wyc_rabbitmq.mapper.OrderMapper;
import com.wyc.wyc_rabbitmq.mapper.OutboxMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class OrderService {

    @Resource
    private OrderMapper orderMapper;
    @Resource
    private OutboxMapper outboxMapper;

    /**
     * 创建订单入口
     */
    public String createOrder(BigDecimal amount) {
        // 1.执行本地事务：新增订单 + 插入outbox待发送记录
        doBizTrans(amount);
        // 事务提交完毕，此处**完全不调用MQ发送代码**
        return "订单创建成功";
    }

    /**
     * 本地事务方法：只操作数据库，无MQ、无短信远程调用
     */
    @Transactional(rollbackFor = Exception.class)
    public void doBizTrans(BigDecimal amount) {
        // 1.新增订单
        String orderNo = UUID.randomUUID().toString();
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setAmount(amount);
        orderMapper.insert(order);

        // 2.插入outbox待发送记录，存储全部MQ参数
        String msgId = UUID.randomUUID().toString();
        Outbox outbox = new Outbox();
        outbox.setMsgId(msgId);
        outbox.setBizType("ORDER_CREATE");
        outbox.setExchange(QueueConstant.ORDER_EXCHANGE);
        outbox.setRoutingKey(QueueConstant.ROUTING_KEY_ORDER_CREATE);
        outbox.setBizData(JSON.toJSONString(order));
        outbox.setSmsContent("您的订单已创建，单号：" + orderNo);
        outbox.setSendStatus("WAIT");
        outbox.setRetryCount(0);
        outboxMapper.insert(outbox);

        // 关键点：事务内部没有任何MqSendUtil.sendMsg调用
    }
}
