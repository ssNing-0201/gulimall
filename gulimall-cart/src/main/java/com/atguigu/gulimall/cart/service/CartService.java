package com.atguigu.gulimall.cart.service;

import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;

import java.util.concurrent.ExecutionException;

public interface CartService {
    /**
     * 添加进购物车
     */
    CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException;
    /**
     * 获取某个购物项
     */
    CartItem getCartitem(Long skuId);
    /**
     * 获取整个购物车
     */
    Cart getCart() throws ExecutionException, InterruptedException;
    /**
     *  清空购物车
     * @param key
     */
    void clearCart(String key);

    /**
     * 勾选购物项
     * @param skuId
     * @param check
     */
    void checkItem(Long skuId, Integer check);

    /**
     * 改变商品数量
     */
    void changeItemCount(Long skuId, Integer num);

    /**
     * 删除购物车中某个商品
     */
    void deleteItem(Long skuId);
}
