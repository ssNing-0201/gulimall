package com.atguigu.gulimall.order.web;

import com.alibaba.fastjson.JSON;
import com.atguigu.gulimall.order.service.OrderService;
import com.atguigu.gulimall.order.vo.OrderConfirmVo;
import com.atguigu.gulimall.order.vo.OrderSubmitVo;
import com.atguigu.gulimall.order.vo.SubmitOrderResponseVo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;

@Controller
public class OrderWebController {

    @Resource
    private OrderService orderService;

    @GetMapping("/toTrade")
    public String toTrade(Model model) throws ExecutionException, InterruptedException {

        OrderConfirmVo orderConfirmVo = orderService.confirmOrder();
        model.addAttribute("OrderConfirmData",orderConfirmVo);
        return "confirm";
    }

    /**
     * 下单功能
     */
    @PostMapping("/submitOrder")
    public String submitOrder(OrderSubmitVo vo, Model model) throws ExecutionException, InterruptedException {
        // 去创建订单，验令牌，验价格，锁库存
        SubmitOrderResponseVo responseVo = orderService.submitOrder(vo);
        if (responseVo.getCode()==0){
            // 下单成功 ，来到支付选择页面
            model.addAttribute("submitOrderResp",responseVo);
            return "pay";
        }else {
            // 下单失败 ，回到订单确认页，重新确认订单信息
            return "redirect:http://order.gulimall.com/toTrade";
        }
    }
}
