package com.wyc.wyc_rabbitmq.controller;

import com.wyc.wyc_rabbitmq.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.annotation.Resource;
import java.math.BigDecimal;

@RestController
public class OrderController {

    @Resource
    private OrderService orderService;

    @GetMapping("/create/order")
    public String create(@RequestParam BigDecimal amount) {
        return orderService.createOrder(amount);
    }
}
