package com.atguigu.gulimall.search.gulimallsearch.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.MatchQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import com.atguigu.gulimall.search.gulimallsearch.constant.EsConstant;
import com.atguigu.gulimall.search.gulimallsearch.services.MallSearchService;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    //去 ES 进行检索
    @Resource
    private ElasticsearchClient client;

    @Override
    public SearchResult search(SearchParam param) {

        SearchResult result = null;

        // 1、动态构建出查询需要的DSL语句
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            // 执行检索请求
            SearchResponse<SearchParam> response = client.search(searchRequest, SearchParam.class);
            // 分析响应数据封装成需要的格式
            result = buildSearchResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建结果数据
     * @return
     */
    private SearchResult buildSearchResult() {


        return null;

    }

    /**
     * 准备检索请求
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {



        MatchQuery matchQuery = new MatchQuery.Builder().build();

        BoolQuery boolQuery = new BoolQuery.Builder().build();
        Query query = new Query.Builder().bool().build();
        SearchRequest searchRequest = new SearchRequest.Builder().index(EsConstant.PRODUCT_INDEX).query(query).build();
        /*
        * MatchQuery matchQuery = new MatchQuery.Builder().field("age").query(30).build();
        Query query = new Query.Builder().match(matchQuery).build();

        SearchRequest searchRequest = new SearchRequest.Builder().query(query).build();
        SearchResponse<Object> search = client.search(searchRequest, Object.class);
        System.out.println("查询结果" + search);


        transport.close();
        * */

        return searchRequest;
    }
}
