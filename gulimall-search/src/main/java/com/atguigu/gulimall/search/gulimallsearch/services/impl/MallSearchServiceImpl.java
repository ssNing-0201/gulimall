package com.atguigu.gulimall.search.gulimallsearch.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.termvectors.Term;
import co.elastic.clients.json.JsonData;
import com.atguigu.gulimall.search.gulimallsearch.constant.EsConstant;
import com.atguigu.gulimall.search.gulimallsearch.services.MallSearchService;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;
import com.mysql.cj.util.StringUtils;
import jakarta.json.Json;
import jakarta.json.JsonString;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
            SearchResponse<Object> response = client.search(searchRequest, Object.class);
            // 分析响应数据封装成需要的格式
            result = buildSearchResult();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建结果数据
     *
     * @return
     */
    private SearchResult buildSearchResult() {


        return null;

    }

    /**
     * 准备检索请求
     *
     * @return
     */
    private SearchRequest buildSearchRequest(SearchParam param) {

        /**
         * 查询
         */
        SearchRequest.Builder searchRequestBuild = new SearchRequest.Builder();
        searchRequestBuild.index(EsConstant.PRODUCT_INDEX);
        BoolQuery.Builder builder = new BoolQuery.Builder();
        // 1、must模糊查询

        if (!StringUtils.isNullOrEmpty(param.getKeyword())) {
            Query byName = MatchQuery.of(m -> m.field("skuTitle").query(param.getKeyword()))._toQuery();
            builder.must(byName);
        }
        // 2、bool - filter -按照三级分类id查询
        if (param.getCatalog3Id() != null) {
            Query byCatalog3Id = TermQuery.of(t -> t.field("catalogId").value(param.getCatalog3Id()))._toQuery();
            builder.filter(byCatalog3Id);
        }
        // 2、bool - filter -按照品牌id查询
        if (param.getBrandId() != null && param.getBrandId().size() > 0) {
            List<FieldValue> fieldValues = new ArrayList<>();
            for (Long s : param.getBrandId()) {
                fieldValues.add(new FieldValue.Builder().longValue(s).build());
            }
            TermsQueryField termsQueryField = new TermsQueryField.Builder().value(fieldValues).build();
            Query byBrandId = TermsQuery.of(t -> t.field("brandId").terms(termsQueryField))._toQuery();
            builder.filter(byBrandId);
        }
        // 3、bool - filter -按照指定属性查询
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<FieldValue> fieldValues = new ArrayList<>();
            String attrId = null;
            for (String attrStr : param.getAttrs()) {
                String[] s = attrStr.split("_");
                attrId = s[0]; // 检索的属性id
                String[] attrValues = s[1].split(":"); // 属性检索用的值
                for (String val : attrValues) {
                    fieldValues.add(new FieldValue.Builder().stringValue(val).build());
                }
                TermsQueryField termsQueryField = new TermsQueryField.Builder().value(fieldValues).build();
                String finalAttrId = attrId;
                Query byAttrs = NestedQuery.of(n -> n.path("attrs").query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("attrs.attrID").value(finalAttrId))).must(m -> m.terms(t -> t.field("attrs.attrValue").terms(termsQueryField))))).scoreMode(null))._toQuery();
                builder.filter(byAttrs);
            }

        }
        // 4、bool - filter -按照是否有库存查询
        Query byHasStock = TermQuery.of(t -> t.field("hasStock").value(param.getHasStock()==1))._toQuery();
        builder.filter(byHasStock);

        // 5、bool - filter -按照价格区间查询查询
        if (!StringUtils.isNullOrEmpty(param.getSkuPrice())) {
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                JsonString min = Json.createValue(s[0] == "" ? "0" : s[0]);
                JsonString max = Json.createValue(s[1]);
                Query bySkuPrice = RangeQuery.of(r -> r.field("skuPrice").gte((JsonData) min).lte((JsonData) max))._toQuery();
                builder.filter(bySkuPrice);
            } else {
                if (!param.getSkuPrice().startsWith("_")) {
                    JsonString min = Json.createValue(s[0]);
                    Query bySkuPrice = RangeQuery.of(r -> r.field("skuPrice").gte((JsonData) min))._toQuery();
                    builder.filter(bySkuPrice);
                }
            }
        }

        // 将之前的查询条件整合一起去查询
        Query query = new Query.Builder().bool(builder.build()).build();
        searchRequestBuild.query(query);
        /**
         * 排序
         */
        SortOptions.Builder sortBuild = new SortOptions.Builder();
        if (!StringUtils.isNullOrEmpty(param.getSort())) {
            String sort = param.getSort();
            String[] s = sort.split("_");
            FieldSort fieldSort = new FieldSort.Builder().field(s[0]).order(s[1].equalsIgnoreCase("asc") ? SortOrder.Asc : SortOrder.Desc).build();
            sortBuild.field(fieldSort);
            searchRequestBuild.sort(sortBuild.build());
        } else { // 默认按价格排序
            FieldSort fieldSort = new FieldSort.Builder().field("skuPrice").order(SortOrder.Asc).build();
            sortBuild.field(fieldSort);
            searchRequestBuild.sort(sortBuild.build());
        }
        // 分页
        Integer page = (param.getPageNum() - 1) * EsConstant.PRODUCT_PAGESIZE;
        searchRequestBuild.from(page).size(EsConstant.PRODUCT_PAGESIZE);
        // 高亮
        Highlight.Builder highlightBuild = new Highlight.Builder();
        if (!StringUtils.isNullOrEmpty(param.getKeyword())) {
            highlightBuild.fields("skuTitle", b -> b.preTags("<b style='color:red'>").postTags("</b>"));
            searchRequestBuild.highlight(highlightBuild.build());
        }
        /**
         * 聚合分析
         */



        /*
        * MatchQuery matchQuery = new MatchQuery.Builder().field("age").query(30).build();
        Query query = new Query.Builder().match(matchQuery).build();

        SearchRequest searchRequest = new SearchRequest.Builder().query(query).build();
        SearchResponse<Object> search = client.search(searchRequest, Object.class);
        System.out.println("查询结果" + search);


        transport.close();
        * */
        SearchRequest searchRequest = searchRequestBuild.build();
        System.out.println(searchRequest.toString());
        return searchRequest;
    }
}
