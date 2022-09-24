package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString
public class SkuItemSaleAttrsVo {
    private Long attrId;
    private String attrName;
    private String attrValues;
}
