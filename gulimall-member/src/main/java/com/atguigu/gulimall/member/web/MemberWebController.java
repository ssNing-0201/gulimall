package com.atguigu.gulimall.member.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.feign.OrderFeginService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Controller
public class MemberWebController {

    @Resource
    private OrderFeginService orderFeginService;

    @GetMapping("/memberOrder.html")
    public String memberOrderPage(@RequestParam(value = "pageNum",defaultValue = "1") Integer pageNum, Model model){

        Map<String,Object> page = new HashMap<>();
        page.put("page",pageNum.toString());
        R r = orderFeginService.listWithItem(page);
        System.out.println(JSON.toJSONString(r));
        model.addAttribute("orders",r);
        return "orderList";
    }

}
