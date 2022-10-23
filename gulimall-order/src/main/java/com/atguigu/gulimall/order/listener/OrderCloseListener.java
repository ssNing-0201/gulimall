package com.atguigu.gulimall.order.listener;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;

@Service
@RabbitListener(queues = "order.release.order.queue")
public class OrderCloseListener {

    @Resource
    private OrderService orderService;

    @RabbitHandler
    public void handlerOrderRelease(OrderEntity entity, Channel channel, Message message) throws IOException {
        System.out.println("收到过期订单信息，准备关闭订单");
        try {
            orderService.closeOrder(entity);
            // 手动调用支付宝收单

            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }catch (Exception e){
            channel.basicReject(message.getMessageProperties().getDeliveryTag(),true);
        }

    }
}
