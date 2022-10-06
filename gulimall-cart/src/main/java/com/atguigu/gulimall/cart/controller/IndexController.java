package com.atguigu.gulimall.cart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

    @GetMapping("/cartlist.html")
    public String index(){
        return "cartList";
    }
    @GetMapping("/success.html")
    public String success(){
        return "success";
    }
}
