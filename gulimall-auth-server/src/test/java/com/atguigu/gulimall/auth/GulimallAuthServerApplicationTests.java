package com.atguigu.gulimall.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Test
    void contextLoads() {

    }
    @Test
    public void getCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int num = (int) (Math.random() * 10);
            builder.append(num);
        }
        System.out.println(builder.toString());
    }

}
