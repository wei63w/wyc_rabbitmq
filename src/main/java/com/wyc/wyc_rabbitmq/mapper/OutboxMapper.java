package com.wyc.wyc_rabbitmq.mapper;

import com.wyc.wyc_rabbitmq.entity.Outbox;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

public interface OutboxMapper {

    @Insert("INSERT INTO t_outbox(msg_id,biz_type,exchange,routing_key,biz_data,sms_content,send_status,retry_count) " +
            "VALUES(#{msgId},#{bizType},#{exchange},#{routingKey},#{bizData},#{smsContent},#{sendStatus},#{retryCount})")
    void insert(Outbox outbox);

    // 查询待发送、重试次数小于3的消息
    @Select("SELECT * FROM t_outbox WHERE send_status = 'WAIT' AND retry_count < #{maxRetry}")
    List<Outbox> selectWaitSendRecord(@Param("maxRetry") Integer maxRetry);

    // 更新发送成功状态
    @Update("UPDATE t_outbox SET send_status='SUCCESS' WHERE id=#{id}")
    void updateSuccess(@Param("id") Long id);

    // 重试次数+1
    @Update("UPDATE t_outbox SET retry_count = retry_count + 1 WHERE id=#{id}")
    void addRetry(@Param("id") Long id);
}
