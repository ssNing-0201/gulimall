package com.atguigu.gulimall.order.listener;


import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

@RabbitListener(queues = "order.seckill.order.queue")
@Component
@Slf4j
public class OrderSeckillListener {

    @Resource
    private OrderService orderService;

    @RabbitHandler
    public void handlerOrderRelease(SeckillOrderTo orderTo, Channel channel, Message message) throws IOException {
        log.info("秒杀单进入，准备创建订单");
        try {
            orderService.createSeckillOrder(orderTo);
            // 手动调用支付宝收单

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
}
