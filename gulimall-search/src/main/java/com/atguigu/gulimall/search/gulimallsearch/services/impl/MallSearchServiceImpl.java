package com.atguigu.gulimall.search.gulimallsearch.services.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldSort;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOptions;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.json.JsonData;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.search.gulimallsearch.constant.EsConstant;
import com.atguigu.gulimall.search.gulimallsearch.feign.ProductFeignService;
import com.atguigu.gulimall.search.gulimallsearch.services.MallSearchService;
import com.atguigu.gulimall.search.gulimallsearch.vo.AttrResponseVo;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchParam;
import com.atguigu.gulimall.search.gulimallsearch.vo.SearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mysql.cj.util.StringUtils;
import jakarta.json.Json;
import jakarta.json.JsonString;
import org.apache.tomcat.util.json.JSONParser;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Kind.Terms;

@Service
public class MallSearchServiceImpl implements MallSearchService {

    //去 ES 进行检索
    @Resource
    private ElasticsearchClient client;
    @Resource
    private ProductFeignService productFeignService;


    @Override
    public SearchResult search(SearchParam param) {

        SearchResult result = null;

        // 1、动态构建出查询需要的DSL语句
        SearchRequest searchRequest = buildSearchRequest(param);
        try {
            // 执行检索请求
            SearchResponse<Object> response = client.search(searchRequest, Object.class);
            // 分析响应数据封装成需要的格式
            result = buildSearchResult(response, param);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * 构建结果数据
     *
     * @param response
     * @return
     */
    private SearchResult buildSearchResult(SearchResponse<Object> response, SearchParam param) throws UnsupportedEncodingException {

        SearchResult result = new SearchResult();
        HitsMetadata<Object> hits = response.hits();
        // 1、返回所有查询到的商品
        List<Hit<Object>> hitList = hits.hits();
        List<SkuEsModel> esModels = new ArrayList<>();
        if (hitList != null && hitList.size() > 0) {
            for (Hit h : hitList) {
                Map<String, Object> source = (Map<String, Object>) h.source();
                String s = new JSONObject(source).toJSONString();
                SkuEsModel esModel = JSON.parseObject(s, SkuEsModel.class);
                if (!StringUtils.isNullOrEmpty(param.getKeyword())) {
                    String skuTitle = h.highlight().get("skuTitle").toString();
                    esModel.setSkuTitle(skuTitle);
                }
                esModels.add(esModel);
            }
        }
        result.setProducts(esModels);
        // 2、当前所有商品涉及到属性
        List<SearchResult.AttrVo> attrVos = new ArrayList<>();
        NestedAggregate attr_agg = response.aggregations().get("attr_agg").nested();
        List<LongTermsBucket> attr_id_agg = attr_agg.aggregations().get("attr_id_agg").lterms().buckets().array();
        for (LongTermsBucket bucket : attr_id_agg) {
            SearchResult.AttrVo attrVo = new SearchResult.AttrVo();
            attrVo.setAttrId(bucket.key());
            attrVo.setAttrName(bucket.aggregations().get("attr_name_agg").sterms().buckets().array().get(0).key());
            List<StringTermsBucket> attr_value_agg = bucket.aggregations().get("attr_value_agg").sterms().buckets().array();
            List<String> attrValue = new ArrayList<>();
            for (StringTermsBucket b : attr_value_agg) {
                attrValue.add(b.key());
            }
            attrVo.setAttrValue(attrValue);
            attrVos.add(attrVo);
        }

        result.setAttrs(attrVos);
        // 3、当前所有商品涉及到品牌
        List<SearchResult.BrandVo> brandVos = new ArrayList<>();
        LongTermsAggregate brand_agg = response.aggregations().get("brand_agg").lterms();
        Buckets<LongTermsBucket> brandBuckets = brand_agg.buckets();
        for (LongTermsBucket bucket : brandBuckets.array()) {
            SearchResult.BrandVo brandVo = new SearchResult.BrandVo();
            brandVo.setBrandId(bucket.key());
            brandVo.setBrandImg(bucket.aggregations().get("brand_img_agg").sterms().buckets().array().get(0).key());
            brandVo.setBrandName(bucket.aggregations().get("brand_name_agg").sterms().buckets().array().get(0).key());
            brandVos.add(brandVo);
        }
        result.setBrands(brandVos);
        // 4、当前所有商品涉及到分类
        LongTermsAggregate catalog_agg = response.aggregations().get("catalog_agg").lterms();
        Buckets<LongTermsBucket> buckets = catalog_agg.buckets();
        List<SearchResult.CatalogVo> catalogVos = new ArrayList<>();
        for (LongTermsBucket bucket : buckets.array()) {
            // 得到分类Id
            long key = bucket.key();
            // 获取分类名字
            String catalog_name_agg = bucket.aggregations().get("catalog_name_agg").sterms().buckets().array().get(0).key();

            SearchResult.CatalogVo catalogVo = new SearchResult.CatalogVo();
            catalogVo.setCatalogId(key);
            catalogVo.setCatalogName(catalog_name_agg);
            catalogVos.add(catalogVo);
        }
        result.setCatalogs(catalogVos);

        //======================以上从聚合信息中获取=========================

        // 1、分页信息-页码
        {
            result.setPageNum(param.getPageNum());
        }
        // 1、分页信息-总记录数
        long total = hits.total().value();
        result.setTotal(total);
        // 1、分页信息-总页码
        int totalPages = (int) total % EsConstant.PRODUCT_PAGESIZE == 0 ? ((int) total / EsConstant.PRODUCT_PAGESIZE) : ((int) total / EsConstant.PRODUCT_PAGESIZE) + 1;
        result.setTotalPages(totalPages);
        // 分页信息-页码
        List<Integer> pages = new ArrayList<>();
        for (int i = 1; i <= totalPages; i++) {
            pages.add(i);
        }
        result.setPages(pages);

        // 构建面包屑导航
        if (param.getAttrs() != null && param.getAttrs().size() > 0) {
            List<SearchResult.NavVo> navVos = new ArrayList<>();
            List<String> attrs = param.getAttrs();
            for (String a : attrs) {
                SearchResult.NavVo navVo = new SearchResult.NavVo();
                String[] s = a.split("_");
                navVo.setNavValue(s[1]);
                navVo.setNavName(s[2]);
                String attr = URLEncoder.encode(a, "UTF-8");
                // 空格，浏览器与java的差异化处理不同 所以要以下步骤 将java识别为 + 的空格替换为浏览器认识的空格 %20
                attr = attr.replace("+","%20");
                String queryString = param.getQueryString().replace("&attrs=" + attr, "");
                navVo.setLink("http://search.gulimall.com/list.html?" + queryString);
                navVos.add(navVo);
            }
            result.setNavs(navVos);
        }
        return result;
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
            Long attrId = null;
            for (String attrStr : param.getAttrs()) {
                String[] s = attrStr.split("_");
                attrId = Long.valueOf(s[0]); // 检索的属性id
                String[] attrValues = s[1].split(":"); // 属性检索用的值
                for (String val : attrValues) {
                    fieldValues.add(new FieldValue.Builder().stringValue(val).build());
                }
                TermsQueryField termsQueryField = new TermsQueryField.Builder().value(fieldValues).build();
                Long finalAttrId = attrId;
                Query byAttrs = NestedQuery.of(n -> n.path("attrs").query(q -> q.bool(b -> b.must(m -> m.term(t -> t.field("attrs.attrId").value(finalAttrId))).must(m -> m.terms(t -> t.field("attrs.attrValue").terms(termsQueryField))))).scoreMode(null))._toQuery();
                builder.filter(byAttrs);
            }

        }
        // 4、bool - filter -按照是否有库存查询
        if (param.getHasStock() != null) {
            Query byHasStock = TermQuery.of(t -> t.field("hasStock").value(param.getHasStock() == 1))._toQuery();
            builder.filter(byHasStock);
        }

        // 5、bool - filter -按照价格区间查询查询
        if (!StringUtils.isNullOrEmpty(param.getSkuPrice())) {
            String[] s = param.getSkuPrice().split("_");
            if (s.length == 2) {
                JsonString min = Json.createValue(s[0] == "" ? "0" : s[0]);
                JsonString max = Json.createValue(s[1]);
                Query bySkuPrice = RangeQuery.of(r -> r.field("skuPrice").gte(JsonData.of(min)).lte(JsonData.of(max)))._toQuery();
                builder.filter(bySkuPrice);
            } else {
                if (!param.getSkuPrice().startsWith("_")) {
                    JsonString min = Json.createValue(s[0]);
                    Query bySkuPrice = RangeQuery.of(r -> r.field("skuPrice").gte(JsonData.of(min)))._toQuery();
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
        // 品牌聚合
        Aggregation.Builder.ContainerBuilder brand_agg = new Aggregation.Builder().terms(t -> t.field("brandId").size(50));
        // 品牌子聚合
        brand_agg.aggregations("brand_name_agg", a -> a.terms(t -> t.field("brandName").size(1)));
        brand_agg.aggregations("brand_img_agg", a -> a.terms(t -> t.field("brandImg").size(1)));
        searchRequestBuild.aggregations("brand_agg", brand_agg.build());
        // 分类聚合
        Aggregation.Builder.ContainerBuilder catalog_agg = new Aggregation.Builder().terms(t -> t.field("catalogId").size(20));
        catalog_agg.aggregations("catalog_name_agg", a -> a.terms(t -> t.field("catalogName").size(10)));
        searchRequestBuild.aggregations("catalog_agg", catalog_agg.build());
        // 属性聚合
        Aggregation.Builder.ContainerBuilder attr_id_agg = new Aggregation.Builder().nested(n -> n.path("attrs")).aggregations("attr_id_agg", a -> a.terms(t -> t.field("attrs.attrId").size(10))
                .aggregations("attr_name_agg", b -> b.terms(t -> t.field("attrs.attrName").size(10)))
                .aggregations("attr_value_agg", c -> c.terms(t -> t.field("attrs.attrValue").size(10)))
        );
        searchRequestBuild.aggregations("attr_agg", attr_id_agg.build());


        SearchRequest searchRequest = searchRequestBuild.build();

        return searchRequest;
    }
}
