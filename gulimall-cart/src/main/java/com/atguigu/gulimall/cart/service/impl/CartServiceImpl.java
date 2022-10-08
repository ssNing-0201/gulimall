package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.Cart;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import com.mysql.cj.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CartServiceImpl implements CartService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ProductFeignService productFeignService;
    @Resource
    ThreadPoolExecutor executor;

    private final String CART_PREFIX = "gulimall:cart:";

    @Override
    public CartItem addToCart(Long skuId, Integer num) throws ExecutionException, InterruptedException {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();

        String o = String.valueOf(cartOps.get(skuId.toString()));
        if ("null".equals(o)) {
            // 购物车无此商品 需要添加
            // 添加新商品到购物车
            CartItem cartItem = new CartItem();
            cartItem.setCheck(true);
            cartItem.setCount(num);
            cartItem.setSkuId(skuId);
            // 1、远程查询当前要添加的商品信息
            CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
                R info = productFeignService.info(skuId);
                Object data = info.get("skuInfo");
                String s = JSON.toJSONString(data);
                SkuInfoVo skuInfoVo = JSON.parseObject(s, SkuInfoVo.class);
                cartItem.setImage(skuInfoVo.getSkuDefaultImg());
                cartItem.setTitle(skuInfoVo.getSkuTitle());
                cartItem.setPrice(skuInfoVo.getPrice());
            }, executor);
            // 3、远程查询sku组合信息
            CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
                List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
                cartItem.setSkuAttr(skuSaleAttrValues);
            }, executor);
            // 等所有异步操作都完成，再进入redis保存数据
            CompletableFuture.allOf(getSkuInfoTask, getSkuSaleAttrValues).get();
            String s = JSON.toJSONString(cartItem);
            cartOps.put(skuId.toString(), s);
            return cartItem;
        } else {
            // 购物车中有此商品，修改数量
            CartItem item = JSON.parseObject(o, CartItem.class);
            item.setCount(item.getCount() + num);
            String s = JSON.toJSONString(item);
            cartOps.put(skuId.toString(), s);
            return item;
        }
    }

    @Override
    public CartItem getCartitem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        String s = String.valueOf(cartOps.get(skuId.toString()));
        CartItem item = JSON.parseObject(s, CartItem.class);
        return item;
    }

    /**
     * 获取购物车内所有商品信息
     *
     * @return
     */
    @Override
    public Cart getCart() throws ExecutionException, InterruptedException {
        // 通过用户信息判断是登陆用户还是临时用户
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        Cart cart = new Cart();
        // 临时购物车数据
        String tempCartKey = CART_PREFIX + userInfoTo.getUserKey();
        List<CartItem> tempItems = getCart(tempCartKey);
        // 用户购物车
        if (userInfoTo.getUserId() != null) {   // 登陆用户
            // 临时购物车有数据，合并到登陆购物车中
            if (tempItems!=null){
                for (CartItem c:tempItems){
                    addToCart(c.getSkuId(),c.getCount());
                }
                clearCart(tempCartKey);
            }
            // 登陆用户的购物车数据
            String cartKey = CART_PREFIX+userInfoTo.getUserId();
            List<CartItem> items = getCart(cartKey);
            cart.setItems(items);
        } else {    // 没登陆
            cart.setItems(tempItems);
        }
        return cart;
    }

    /**
     * 获取指定 key 内购物车数据
     * @param cartKey
     * @return
     */
    @Nullable
    private List<CartItem> getCart(String cartKey) {
        BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(cartKey);
        List<Object> values = hashOps.values();
        if (values != null && values.size() > 0) {
            List<CartItem> collect= values.stream().map(obj -> {
                String str = String.valueOf(obj);
                CartItem cartItem = JSON.parseObject(str, CartItem.class);
                return cartItem;
            }).collect(Collectors.toList());
            return collect;
        }
        return null;
    }

    // 将获取要操作的购物车，抽象为一个方法 购物车用redis保存。返回的是一个可操作的 redis ops 对象
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId() != null) {
            cartKey = CART_PREFIX + userInfoTo.getUserId();
        } else {
            cartKey = CART_PREFIX + userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(cartKey);
        return operations;
    }

    @Override
    public void clearCart(String key){
        stringRedisTemplate.delete(key);
    }

    @Override
    public void checkItem(Long skuId, Integer check) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartitem = getCartitem(skuId);
        cartitem.setCheck(check==1?true:false);
        String s = JSON.toJSONString(cartitem);
        cartOps.put(skuId.toString(),s);
    }

    @Override
    public void changeItemCount(Long skuId, Integer num) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        CartItem cartitem = getCartitem(skuId);
        cartitem.setCount(num);
        cartOps.put(skuId.toString(),JSON.toJSONString(cartitem));
    }

    @Override
    public void deleteItem(Long skuId) {
        BoundHashOperations<String, Object, Object> cartOps = getCartOps();
        cartOps.delete(skuId.toString());
    }
}
