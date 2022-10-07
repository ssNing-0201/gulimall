package com.atguigu.gulimall.cart.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@FeignClient("gulimail-product")
public interface ProductFeignService {
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);

    @RequestMapping("/product/skusaleattrvalue/stringlist/{skuid}")
    public List<String> getSkuSaleAttrValues(@PathVariable("skuid") Long skuid);
}
