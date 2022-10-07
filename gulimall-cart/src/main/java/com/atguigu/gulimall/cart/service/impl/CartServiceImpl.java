package com.atguigu.gulimall.cart.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.cart.feign.ProductFeignService;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.SkuInfoVo;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

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
        // 1、远程查询当前要添加的商品信息
        CartItem cartItem = new CartItem();
        cartItem.setCheck(true);
        cartItem.setCount(num);
        cartItem.setSkuId(skuId);

        CompletableFuture<Void> getSkuInfoTask = CompletableFuture.runAsync(() -> {
            R info = productFeignService.info(skuId);
            Object data = info.get("skuInfo");
            String s = JSON.toJSONString(data);
            SkuInfoVo skuInfoVo = JSON.parseObject(s, SkuInfoVo.class);
            cartItem.setImage(skuInfoVo.getSkuDefaultImg());
            cartItem.setTitle(skuInfoVo.getSkuTitle());
            cartItem.setPrice(skuInfoVo.getPrice());
        },executor);
        // 3、远程查询sku组合信息
        CompletableFuture<Void> getSkuSaleAttrValues = CompletableFuture.runAsync(() -> {
            List<String> skuSaleAttrValues = productFeignService.getSkuSaleAttrValues(skuId);
            cartItem.setSkuAttr(skuSaleAttrValues);
        },executor);
        // 等所有异步操作都完成，再进入redis保存数据
        CompletableFuture.allOf(getSkuInfoTask,getSkuSaleAttrValues).get();
        String s = JSON.toJSONString(cartItem);
        cartOps.put(skuId.toString(),s);
        return cartItem;
    }
    // 将获取要操作的购物车，抽象为一个方法 购物车用redis保存。返回的是一个可操作的 redis ops 对象
    private BoundHashOperations<String, Object, Object> getCartOps() {
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();
        String cartKey = "";
        if (userInfoTo.getUserId()!=null){
            cartKey = CART_PREFIX+userInfoTo.getUserId();
        }else {
            cartKey = CART_PREFIX+userInfoTo.getUserKey();
        }

        BoundHashOperations<String, Object, Object> operations = stringRedisTemplate.boundHashOps(cartKey);
        return operations;
    }
}
