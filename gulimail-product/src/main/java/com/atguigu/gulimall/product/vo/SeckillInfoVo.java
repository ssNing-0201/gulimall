package com.atguigu.gulimall.product.vo;

import lombok.Data;
import lombok.ToString;

import java.math.BigDecimal;

@Data
@ToString
public class SeckillInfoVo {
    private Long id;

    private Long promotionId;

    private Long promotionSessionId;

    private Long skuId;

    private BigDecimal seckillPrice;

    private Integer seckillCount;

    private Integer seckillLimit;

    private Integer seckillSort;

    // 秒杀开始时间
    private Long startTime;
    // 秒杀结束时间
    private Long endTime;
    // 秒杀随机码
    private String randomCode;
}
