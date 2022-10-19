package com.atguigu.gulimall.order.config;

import com.atguigu.gulimall.order.entity.OrderEntity;
import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class MyMqConfig {


    @RabbitListener(queues = "order.release.order.queue")
    public void listener(OrderEntity uuid, Channel channel, Message message) throws IOException {
        System.out.println("收到过期订单信息，准备关闭订单"+uuid.getOrderSn());
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    // 队列
    @Bean // 加上 @Bean 容器中的这些都会自动创建(前提是mq中没有)
    public Queue orderDelayQueue(){

        Map<String,Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange","order-event-exchange");
        arguments.put("x-dead-letter-routing-key","order.release.order");
        arguments.put("x-message-ttl",60000);
        Queue queue = new Queue("order.delay.queue", true, false, false,arguments);
        return queue;
    }
    // 队列
    @Bean
    public Queue orderReleaseOrderQueue(){
        Queue queue = new Queue("order.release.order.queue", true, false, false);
        return queue;
    }
    // 交换机
    @Bean
    public TopicExchange orderEventExchange(){
        TopicExchange topicExchange = new TopicExchange("order-event-exchange", true, false);
        return topicExchange;
    }
    // 绑定关系
    @Bean
    public Binding orderCreateOrderBinding(){
        Binding binding = new Binding("order.delay.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.create.order",
                null);
        return binding;
    }
    // 绑定关系
    @Bean
    public Binding orderReleaseOrderBinding(){
        Binding binding = new Binding("order.release.order.queue",
                Binding.DestinationType.QUEUE,
                "order-event-exchange",
                "order.releases.order", null);
        return binding;
    }

}
