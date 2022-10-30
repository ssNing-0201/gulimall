package com.atguigu.common.to.mq;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class SeckillOrderTo {

    // 订单号
    private String orderSn;
    // 活动场次Id
    private Long promotionSessionId;
    // 商品Id
    private Long skuId;
    // 秒杀价格
    private BigDecimal seckillPrice;
    // 购买数量
    private Integer num;
    // 会员Id
    private Long memberId;

}
