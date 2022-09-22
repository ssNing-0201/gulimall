package com.atguigu.gulimall.search.gulimallsearch.vo;

import co.elastic.clients.elasticsearch.nodes.Ingest;
import com.atguigu.common.to.es.SkuEsModel;
import lombok.Data;

import java.util.List;

@Data
public class SearchResult {

    // 查询到的商品信息
    private List<SkuEsModel> products;
    /**
     * 以下是分页信息
     */
    private Integer pageNum; // 当前页码
    private Long total; // 总记录数
    private Integer totalPages; // 总页码

    private List<BrandVo> brands; // 当前查询结果，所有涉及的品牌信息
    private List<AttrVo> attrs; // 查询到的所有涉及的属性。
    private List<CatalogVo> catalogs; // 当前查询结果所涉及到的分类信息
    private List<Integer> pages; // 页码

    //===================以上是返回给页面的所有信息========================
    private List<NavVo> navs;

    @Data
    public static class NavVo{
        private String navName;
        private String navValue;
        private String link;
    }

    //===================以上是面包屑导航的所有信息========================

    @Data
    public static class BrandVo{

        private Long brandId;
        private String brandName;
        private String brandImg;
    }
    @Data
    public static class AttrVo{

        private Long attrId;
        private String attrName;
        private List<String> attrValue;
    }
    @Data
    public static class CatalogVo{

        private Long catalogId;
        private String catalogName;
    }
}
