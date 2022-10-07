package com.atguigu.gulimall.cart.controller;


import com.atguigu.common.constant.AuthServerContant;
import com.atguigu.gulimall.cart.interceptor.CartInterceptor;
import com.atguigu.gulimall.cart.service.CartService;
import com.atguigu.gulimall.cart.vo.CartItem;
import com.atguigu.gulimall.cart.vo.UserInfoTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Resource
    private CartService cartService;

    @GetMapping("/cart.html")
    public String cartListPage(HttpSession session){

        // 快速得到用户信息
        UserInfoTo userInfoTo = CartInterceptor.threadLocal.get();

        return "cartList";
    }

    /**
     * 添加商品到购物车
     * @return
     */
    @GetMapping("/addToCart")
    public String addToCart(@RequestParam("skuId") Long skuId, @RequestParam("num") Integer num, Model model) throws ExecutionException, InterruptedException {
        CartItem cartItem = cartService.addToCart(skuId,num);
        model.addAttribute("item",cartItem);
        return "success";
    }
}
