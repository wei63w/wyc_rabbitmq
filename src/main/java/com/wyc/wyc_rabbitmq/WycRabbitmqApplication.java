package com.wyc.wyc_rabbitmq;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wyc.wyc_rabbitmq.mapper")
public class WycRabbitmqApplication {

    public static void main(String[] args) {
        SpringApplication.run(WycRabbitmqApplication.class, args);
    }

}
