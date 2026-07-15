package com.wyc.wyc_rabbitmq.task;

import com.wyc.wyc_rabbitmq.entity.Outbox;
import com.wyc.wyc_rabbitmq.mapper.OutboxMapper;
import com.wyc.wyc_rabbitmq.util.MqSendUtil;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

//@Component
//@EnableScheduling
public class OutboxTask {

    @Resource
    private OutboxMapper outboxMapper;
    @Resource
    private MqSendUtil mqSendUtil;

    // 每5秒扫描一次待发送消息
    @Scheduled(fixedRate = 5000)
    public void scanAndSend() {
        // 查询待发送、最多重试3次
        List<Outbox> waitList = outboxMapper.selectWaitSendRecord(3);
        if (waitList.isEmpty()) {
            return;
        }

        for (Outbox record : waitList) {
            try {
                // ========================
                // 唯一调用MQ投递的地方！
                // 执行时机：订单事务早已提交完成
                // sendMsg同步阻塞，等待MQ投递成功再执行短信
                // ========================
                mqSendUtil.sendMsg(
                        record.getExchange(),
                        record.getRoutingKey(),
                        record.getBizData(),
                        record.getMsgId()
                );

                // 模拟调用短信接口
                System.out.println("发送短信：" + record.getSmsContent());

                // MQ+短信全部成功，更新状态
                outboxMapper.updateSuccess(record.getId());
            } catch (Exception e) {
                // 发送失败，重试次数+1，下一轮定时任务重试（会产生重复投递）
                outboxMapper.addRetry(record.getId());
                System.err.println("消息投递失败 msgId=" + record.getMsgId() + " 异常：" + e.getMessage());
            }
        }
    }
}
