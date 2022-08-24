package com.atguigu.gulimall.search.gulimallsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;


@SpringBootTest
public class GulimallSearchApplicationTests {

    @Resource
    private ElasticsearchClient client;

    @Test
    public void contextLoads() {
        System.out.println(client);
    }

}
