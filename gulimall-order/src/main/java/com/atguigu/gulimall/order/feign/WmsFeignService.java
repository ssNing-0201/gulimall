package com.atguigu.gulimall.order.feign;

import com.atguigu.gulimall.order.vo.SkuHasStockVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient("gulimall-ware")
public interface WmsFeignService {

    @PostMapping("/ware/waresku/hasstock")
    public List<SkuHasStockVo> getSkusHasStock(@RequestBody List<Long> skuIds);
}
