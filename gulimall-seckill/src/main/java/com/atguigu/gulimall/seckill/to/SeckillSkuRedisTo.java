package com.atguigu.gulimall.seckill.to;

import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class SeckillSkuRedisTo {

    private Long id;

    private Long promotionId;

    private Long promotionSessionId;

    private Long skuId;

    private BigDecimal seckillPrice;

    private Integer seckillCount;

    private Integer seckillLimit;

    private Integer seckillSort;

    // Sku 的详细信息
    private SkuInfoVo skuInfo;
    // 秒杀开始时间
    private Long startTime;
    // 秒杀结束时间
    private Long endTime;
    // 秒杀随机码
    private String randomCode;


}
