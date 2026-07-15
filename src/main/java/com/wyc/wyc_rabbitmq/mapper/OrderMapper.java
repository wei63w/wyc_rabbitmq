package com.wyc.wyc_rabbitmq.mapper;

import com.wyc.wyc_rabbitmq.entity.Order;
import org.apache.ibatis.annotations.Insert;

public interface OrderMapper {
    @Insert("INSERT INTO t_order(order_no, amount) VALUES(#{orderNo}, #{amount})")
    void insert(Order order);
}
