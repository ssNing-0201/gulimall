package com.atguigu.gulimall.cart.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 整个购物车
 * 需要计算的属性，必须重写get方法。
 */
public class Cart {
    private List<CartItem> items;
    private Integer countNum; // 商品数量
    private Integer countType; // 商品类型
    private BigDecimal totalAmount; // 商品总价
    private BigDecimal reduce = new BigDecimal("0.00"); // 减免价格

    public List<CartItem> getItems() {
        return items;
    }

    public void setItems(List<CartItem> items) {
        this.items = items;
    }

    public Integer setCountNum(){
        return this.countNum = getCountNum();
    }
    public Integer getCountNum() {
        int count = 0;
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                count += item.getCount();
            }
        }
        return count;
    }

    public Integer setCountType(){
        return this.countType = getCountType();
    }
    public Integer getCountType() {
        return items.size();
    }
    public BigDecimal setTotalAmount(){
        return this.totalAmount = getTotalAmount();
    }
    public BigDecimal getTotalAmount() {
        BigDecimal amount = new BigDecimal(0.00);
        // 计算商品总价
        List<CartItem> items = getItems();
        if (items != null && items.size() > 0) {
            for (CartItem item : items) {
                if (item.getCheck()){
                    amount = amount.add(item.getTotalPrice());
                }
            }
        }
        // 减去优惠总价
        amount.subtract(getReduce());
        return amount;
    }

    public BigDecimal getReduce() {
        return reduce;
    }

    public void setReduce(BigDecimal reduce) {
        this.reduce = reduce;
    }

    @Override
    public String toString() {
        return "Cart{" +
                "items=" + items +
                ", countNum=" + countNum +
                ", countType=" + countType +
                ", totalAmount=" + totalAmount +
                ", reduce=" + reduce +
                '}';
    }
}
