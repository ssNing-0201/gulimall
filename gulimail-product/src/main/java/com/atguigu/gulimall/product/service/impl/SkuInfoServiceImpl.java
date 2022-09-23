package com.atguigu.gulimall.product.service.impl;

import com.atguigu.gulimall.product.entity.SkuImagesEntity;
import com.atguigu.gulimall.product.entity.SpuInfoDescEntity;
import com.atguigu.gulimall.product.service.AttrGroupService;
import com.atguigu.gulimall.product.service.SkuImagesService;
import com.atguigu.gulimall.product.service.SpuInfoDescService;
import com.atguigu.gulimall.product.vo.SkuItemVo;
import com.atguigu.gulimall.product.vo.SpuItemGroupVo;
import com.mysql.cj.util.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.product.dao.SkuInfoDao;
import com.atguigu.gulimall.product.entity.SkuInfoEntity;
import com.atguigu.gulimall.product.service.SkuInfoService;

import javax.annotation.Resource;


@Service("skuInfoService")
public class SkuInfoServiceImpl extends ServiceImpl<SkuInfoDao, SkuInfoEntity> implements SkuInfoService {

    @Resource
    private SkuImagesService skuImagesService;
    @Resource
    private SpuInfoDescService spuInfoDescService;
    @Resource
    private AttrGroupService attrGroupService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                new QueryWrapper<SkuInfoEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void saveSkuInfo(SkuInfoEntity skuInfoEntity) {
        this.baseMapper.insert(skuInfoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {

        QueryWrapper<SkuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if (!StringUtils.isNullOrEmpty(key)) {
            wrapper.and(w -> {
                w.eq("sku_id", params.get("key")).or().like("sku_name", params.get("key"));
            });
        }
        String catelogId = (String) params.get("catelogId");
        if (!StringUtils.isNullOrEmpty(catelogId)) {
            if (!"0".equals(catelogId)) {
                wrapper.eq("catalog_id", params.get("catelogId"));
            }
        }
        String brandId = (String) params.get("brandId");
        if (!StringUtils.isNullOrEmpty(brandId)) {
            if (!"0".equals(brandId)) {
                wrapper.eq("brand_id", params.get("brandId"));
            }
        }
        String min = (String) params.get("min");
        if (!StringUtils.isNullOrEmpty(min)) {
            wrapper.ge("price", params.get("min"));
        }
        String max = (String) params.get("max");
        BigDecimal bigDecimal = new BigDecimal(max);
        if (!StringUtils.isNullOrEmpty(max)) {
            if (bigDecimal.compareTo(new BigDecimal("0")) == 1) {
                wrapper.le("price", params.get("max"));
            }
        }

        IPage<SkuInfoEntity> page = this.page(
                new Query<SkuInfoEntity>().getPage(params),
                wrapper
        );

        return new PageUtils(page);
    }

    @Override
    public List<SkuInfoEntity> getSkusBySpuId(Long spuId) {

        List<SkuInfoEntity> skuInfoEntities = this.list(new QueryWrapper<SkuInfoEntity>().eq("spu_id",spuId));

        return skuInfoEntities;
    }

    @Override
    public SkuItemVo item(Long skuId) {

        SkuItemVo skuItemVo = new SkuItemVo();
        // 1、sku基本信息获取 pms_sku_info
        SkuInfoEntity infoEntity = getById(skuId);
        skuItemVo.setInfo(infoEntity);
        // 2、sku图片信息 psm_sku_images
        List<SkuImagesEntity> images = skuImagesService.getImagesBySkuId(skuId);
        skuItemVo.setImages(images);
        // 3、获取spu的销售属性销售属性组合

        // 4、获取spu介绍
        Long spuId = infoEntity.getSpuId();
        SpuInfoDescEntity spuInfoDesc = spuInfoDescService.getById(spuId);
        skuItemVo.setDesc(spuInfoDesc);
        // 5、获取spu规格参数信息。
        Long catalogId = infoEntity.getCatalogId();
        List<SpuItemGroupVo> groupAttrs = attrGroupService.getAttrGroupWithAttrsBySpuId(spuId,catalogId);

        skuItemVo.setGroupAttrs(groupAttrs);

        return skuItemVo;
    }

}