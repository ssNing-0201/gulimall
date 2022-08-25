package com.atguigu.gulimall.search.gulimallsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;

import com.atguigu.gulimall.search.gulimallsearch.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.io.IOException;


@SpringBootTest
public class GulimallSearchApplicationTests {

    @Resource
    private ElasticsearchClient client;



    @Test
    public void searchData() {
        SearchResponse<User> search = null;
        try {
            search = client.search(s -> s
                    .index("user")
                    .query(q -> q
                            .term(t -> t
                                    .field("name")
                                    .value(v->v.stringValue("zhangsan"))
                            )
                    ),
                    User.class
            );
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (Hit<User> hit: search.hits().hits()){
            User user = hit.source();
            System.out.println(user);
        }
    }


    @Test
    public void contextLoads() {

        User user = new User("zhangsan", "18", "男");

        IndexRequest.Builder<User> indexReqBuilder = new IndexRequest.Builder<>();
        indexReqBuilder.index("user");
        indexReqBuilder.id(user.getAge());
        indexReqBuilder.document(user);
        try {
            IndexResponse response = client.index(indexReqBuilder.build());
            System.out.println(response);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
