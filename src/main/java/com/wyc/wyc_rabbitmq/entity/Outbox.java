package com.wyc.wyc_rabbitmq.entity;

import lombok.Data;

@Data
public class Outbox {
    private Long id;
    private String msgId;
    private String bizType;
    private String exchange;
    private String routingKey;
    private String bizData;
    private String smsContent;
    private String sendStatus;
    private Integer retryCount;
    // getter setter
}