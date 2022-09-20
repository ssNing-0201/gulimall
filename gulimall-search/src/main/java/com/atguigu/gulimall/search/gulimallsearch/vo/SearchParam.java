package com.atguigu.gulimall.search.gulimallsearch.vo;


import lombok.Data;

import java.util.List;

/**
 * 封装页面所有可能传递过来的查询条件
 */
@Data
public class SearchParam {

//    private Long spuId;
//    private Long skuId;
//    private String skuTitle;
//    private String skuImg;
//    private boolean saleCount;

    private String keyword; // 页面传递过来的检索参数，全文匹配关键字
    private Long catalog3Id; // 三级分类Id
    private String sort; // 排序条件
    private Integer hasStock; // 是否只显示有货
    private String skuPrice; // 价格区间查询
    private List<Long> brandId; // 品牌id
    private List<String> attrs; // 按照属性进行筛选
    private Integer pageNum = 1; // 页码
}
