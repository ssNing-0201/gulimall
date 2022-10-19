package com.atguigu.gulimall.order.web;

import com.atguigu.gulimall.order.entity.OrderEntity;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;
import java.util.Date;
import java.util.UUID;

@Controller
public class HelloController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("/test/createOrder")
    @ResponseBody
    public String createOrderTest(){
        // 创建订单
        OrderEntity orderEntity = new OrderEntity();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        orderEntity.setOrderSn(UUID.randomUUID().toString().replace("-",uuid));
        orderEntity.setModifyTime(new Date());
        // 给 MQ 发送消息
        rabbitTemplate.convertAndSend("order-event-exchange","order.create.order",orderEntity);
        return "ok";
    }

    @GetMapping("/{page}.html")
    public String listPage(@PathVariable("page") String page){
        return page;
    }
}
