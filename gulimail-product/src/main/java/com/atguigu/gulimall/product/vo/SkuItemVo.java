package com.atguigu.gulimall.product.vo;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import lombok.Data;
import lombok.ToString;

import java.util.List;
@ToString
@Data
public class SkuItemVo {
    // 1、sku基本信息获取 pms_sku_info
    private SkuInfoEntity info;
    private Boolean hasStock = true;
    // 2、sku图片信息 psm_sku_images
    private List<SkuImagesEntity> images;
    // 3、获取spu的销售属性销售属性组合
    private List<SkuItemSaleAttrsVo> saleAttr;
    // 4、获取spu介绍
    private SpuInfoDescEntity desc;
    // 5、获取spu规格参数信息。
    private List<SpuItemGroupVo> groupAttrs ;

}
