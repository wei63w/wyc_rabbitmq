CREATE TABLE `t_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no` varchar(64) NOT NULL COMMENT '订单编号',
  `amount` decimal(10,2) NOT NULL COMMENT '订单金额',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE `t_outbox` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `msg_id` varchar(64) NOT NULL COMMENT '全局唯一消息ID，幂等使用',
  `biz_type` varchar(32) NOT NULL COMMENT '业务类型 ORDER_CREATE',
  `exchange` varchar(64) NOT NULL COMMENT 'MQ交换机',
  `routing_key` varchar(64) NOT NULL COMMENT '路由key',
  `biz_data` text COMMENT 'MQ消息JSON体',
  `sms_content` varchar(255) DEFAULT NULL COMMENT '短信通知内容',
  `send_status` varchar(16) NOT NULL DEFAULT 'WAIT' COMMENT 'WAIT待发送 SUCCESS成功 FAIL失败',
  `retry_count` int DEFAULT '0' COMMENT '重试次数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `msg_id` (`msg_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
