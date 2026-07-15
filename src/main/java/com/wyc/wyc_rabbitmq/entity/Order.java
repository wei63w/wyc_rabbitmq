package com.wyc.wyc_rabbitmq.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {
    private Long id;
    private String orderNo;
    private BigDecimal amount;
    private LocalDateTime createTime;
    // getter setter
}
