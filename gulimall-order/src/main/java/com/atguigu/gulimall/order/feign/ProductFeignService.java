package com.atguigu.gulimall.order.feign;

import com.atguigu.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("gulimail-product")
public interface ProductFeignService {

    @GetMapping("/product/spuinfo/getSpuBySku/{skuId}")
    public R getSpuBySku(@PathVariable("skuId") Long skuId);
}
