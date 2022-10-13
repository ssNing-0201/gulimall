package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderSubmitVo {

    private Long addrId; // 收货地址id
    private Integer payType; // 支付方式
    // 无需提交需要购买的商品，去购物车获取
    // 优惠 发票 没做
    private String orderToken; // 令牌，防重
    private BigDecimal payPrice; // 应付价格，验价用
    private String note; // 订单备注信息
}
