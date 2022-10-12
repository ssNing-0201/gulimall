package com.atguigu.gulimall.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


public class OrderConfirmVo {

    // 收获地址列表
    private List<MemberAddressVo> address;
    // 购物项
    private List<OrderItemVo> items;
    // 发票记录

    // 优惠卷信息
    private Integer integration;
    // 是否有库存
    private Map<Long,Boolean> stocks;
    // 订单总额
    private BigDecimal total;
    // 应付价格
    private BigDecimal payPrice;
    // 总件数
    private Integer totalCount;

    public Map<Long, Boolean> getStocks() {
        return stocks;
    }

    public void setStocks(Map<Long, Boolean> stocks) {
        this.stocks = stocks;
    }

    public Integer getTotalCount() {
        int count = 0;
        if (items!=null&&items.size()>0){
            for (OrderItemVo item:items){
                count+=item.getCount();
            }
        }
        return count;
    }

    public void setTotalCount() {
        this.totalCount = getTotalCount();
    }

    public String getOrderToken() {
        return orderToken;
    }

    public void setOrderToken(String orderToken) {
        this.orderToken = orderToken;
    }

    // 订单防重令牌
    private String orderToken;

    public List<MemberAddressVo> getAddress() {
        return address;
    }

    public void setAddress(List<MemberAddressVo> address) {
        this.address = address;
    }

    public List<OrderItemVo> getItems() {
        return items;
    }

    public void setItems(List<OrderItemVo> items) {
        this.items = items;
    }

    public Integer getIntegration() {
        int temp;
        BigDecimal pay = new BigDecimal(0.00);
        if (integration > 200) {
            temp =  20;
        } else {
            temp = integration/10;
        }
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                pay = pay.add(item.getTotalPrice());
            }
        } else {
            pay = BigDecimal.valueOf(0);
        }
        if (pay.compareTo(BigDecimal.valueOf(temp))==1){
            return temp;
        }else {
            return 0;
        }
    }

    public void setIntegration(Integer integration) {
        this.integration = integration;
    }

    public BigDecimal getTotal() {
        BigDecimal total = new BigDecimal(0.00);
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                total = total.add(item.getTotalPrice());
            }
        }
        return total;
    }

    public void setTotal() {
        this.total = getTotal();
    }

    public BigDecimal getPayPrice() {
        BigDecimal payPrice = new BigDecimal(0.00);
        BigDecimal discount = new BigDecimal(getIntegration());
        if (items != null && items.size() > 0) {
            for (OrderItemVo item : items) {
                payPrice = payPrice.add(item.getTotalPrice());
            }
        } else {
            return new BigDecimal(0.00);
        }
        payPrice = payPrice.subtract(discount);
        return payPrice;
    }

    public void setPayPrice() {
        this.payPrice = getPayPrice();
    }
}
