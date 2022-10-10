package com.atguigu.gulimall.order;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class GulimallOrderApplicationTests {

    @Resource
    private AmqpAdmin amqpAdmin;

    @Test
    void contextLoads() {
    }

}
