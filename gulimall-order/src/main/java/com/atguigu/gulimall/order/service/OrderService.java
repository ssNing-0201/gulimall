package com.atguigu.gulimall.order.service;

import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.order.entity.OrderEntity;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * ????
 *
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 20:51:09
 */
public interface OrderService extends IService<OrderEntity> {

    PageUtils queryPage(Map<String, Object> params);
    /**
     * 订单确认页所需返回信息
     */
    OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException;

    /**
     * 下单
     */
    SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) throws ExecutionException, InterruptedException;

    /**
     * 根据订单号查询订单
     */
    OrderEntity getOrderByOrderSn(String orderSn);

    void closeOrder(OrderEntity entity);

    PayVo getOrderPay(String orderSn);

    PageUtils queryPageWithItem(Map<String, Object> params);

    String handlePayResult(PayAsyncVo vo);

    void createSeckillOrder(SeckillOrderTo orderTo);
}

