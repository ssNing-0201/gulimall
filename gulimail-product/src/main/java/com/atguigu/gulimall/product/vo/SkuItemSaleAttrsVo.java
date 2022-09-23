package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SkuItemSaleAttrsVo {
    private Long attrid;
    private String attrName;
    private List<String> attrValues;
}
