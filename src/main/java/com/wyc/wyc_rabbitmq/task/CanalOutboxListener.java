package com.wyc.wyc_rabbitmq.task;
import com.alibaba.fastjson2.JSON;
import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.protocol.CanalEntry;
import com.alibaba.otter.canal.protocol.Message;
import com.google.protobuf.InvalidProtocolBufferException;
import com.wyc.wyc_rabbitmq.entity.Outbox;
import com.wyc.wyc_rabbitmq.mapper.OutboxMapper;
import com.wyc.wyc_rabbitmq.util.MqSendUtil;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class CanalOutboxListener {

    private static final Logger log = LoggerFactory.getLogger(CanalOutboxListener.class);

    @Value("${canal.host}")
    private String canalHost;
    @Value("${canal.port}")
    private int canalPort;
    @Value("${canal.destination}")
    private String destination;

    @Value("${canal.username}")
    private String username;
    @Value("${canal.password}")
    private String password;

    @Resource
    private OutboxMapper outboxMapper;
    @Resource
    private MqSendUtil mqSendUtil;

    private CanalConnector connector;
    private ExecutorService executor;
    private volatile boolean running = true;

    // 项目启动自动开启监听
    @PostConstruct
    public void startListen() {
        connector = CanalConnectors.newSingleConnector(
                new InetSocketAddress(canalHost, canalPort),
                destination, username, password
        );
        executor = Executors.newSingleThreadExecutor();
        executor.execute(this::listenSafely);
        log.info("Canal listener started: server={}:{}, destination={}, filter=test\\.t_outbox",
                canalHost, canalPort, destination);
    }

    private void listenSafely() {
        try {
            loopReadBinlog();
        } catch (Exception e) {
            log.error("Canal listener stopped unexpectedly. Check Canal server address and client credentials.", e);
        }
    }

    // 循环拉取binlog数据
    public void loopReadBinlog() {
        connector.connect();
        // 监听表，每次从头抓取
        connector.subscribe("test.t_outbox");
        while (running) {
            // 每次拉取1000条binlog数据
            Message message = connector.getWithoutAck(1000);
            long batchId = message.getId();
            int size = message.getEntries().size();
            if (batchId == -1 || size == 0) {
                continue;
            }
            try {
                handleBinlogData(message.getEntries());
                // 处理完成，确认位点，下次不会重复拉取
                connector.ack(batchId);
            } catch (Exception e) {
                // 处理失败，不ack，下次重新消费这批binlog
                connector.rollback(batchId);
                e.printStackTrace();
            }
        }
    }

    // 解析binlog行数据，只处理INSERT（新增outbox记录）
    public void handleBinlogData(List<CanalEntry.Entry> entryList) throws InvalidProtocolBufferException {
        for (CanalEntry.Entry entry : entryList) {
            // 只处理表数据变更，过滤DDL语句
            if (entry.getEntryType() != CanalEntry.EntryType.ROWDATA) {
                continue;
            }
            CanalEntry.RowChange rowChange = CanalEntry.RowChange.parseFrom(entry.getStoreValue());
            // 只捕获插入操作，update/delete忽略
            if (rowChange.getEventType() != CanalEntry.EventType.INSERT) {
                continue;
            }
            // 遍历新增行
            for (CanalEntry.RowData rowData : rowChange.getRowDatasList()) {
                parseAndSendMq(rowData);
            }
        }
    }

    // 解析行字段，封装Outbox，发送MQ
    public void parseAndSendMq(CanalEntry.RowData rowData) {
        Outbox outbox = new Outbox();
        // 遍历所有字段赋值
        for (CanalEntry.Column column : rowData.getAfterColumnsList()) {
            String colName = column.getName();
            String value = column.getValue();
            switch (colName) {
                case "id":
                    outbox.setId(Long.valueOf(value));
                    break;
                case "msg_id":
                    outbox.setMsgId(value);
                    break;
                case "exchange":
                    outbox.setExchange(value);
                    break;
                case "routing_key":
                    outbox.setRoutingKey(value);
                    break;
                case "biz_data":
                    outbox.setBizData(value);
                    break;
                case "sms_content":
                    outbox.setSmsContent(value);
                    break;
                case "send_status":
                    outbox.setSendStatus(value);
                    break;
                case "retry_count":
                    outbox.setRetryCount(Integer.valueOf(value));
                    break;
            }
        }

        // 发送RabbitMQ
        try {
            mqSendUtil.sendMsg(
                    outbox.getExchange(),
                    outbox.getRoutingKey(),
                    outbox.getBizData(),
                    outbox.getMsgId()
            );
            System.out.println("Canal捕获outbox，投递MQ成功 msgId:" + outbox.getMsgId());
            // 更新状态为已发送
            outboxMapper.updateSuccess(outbox.getId());
        } catch (Exception e) {
            // 发送失败，重试次数+1，下次binlog不会重复触发，靠数据库兜底重试
            outboxMapper.addRetry(outbox.getId());
            System.err.println("Canal投递MQ失败 msgId:" + outbox.getMsgId());
        }
    }

    // 项目关闭销毁资源
    @PreDestroy
    public void stop() {
        running = false;
        if (executor != null) executor.shutdown();
        if (connector != null) connector.disconnect();
    }
}
