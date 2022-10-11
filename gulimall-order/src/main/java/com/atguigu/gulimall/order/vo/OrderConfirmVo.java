package com.atguigu.gulimall.order.vo;

import java.math.BigDecimal;
import java.util.List;

public class OrderConfirmVo {

    // 收获地址列表
    private List<MemberAddressVo> address;
    // 购物项
    private List<OrderItemVo> items;
    // 发票记录

    // 优惠卷信息
    private Integer integration;
    // 订单总额
    private BigDecimal total;
    // 应付价格
    private BigDecimal payPrice;
}
