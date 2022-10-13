package com.atguigu.gulimall.order.service.impl;

import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.vo.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.order.dao.OrderDao;
import com.atguigu.gulimall.order.entity.OrderEntity;
import com.atguigu.gulimall.order.service.OrderService;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import javax.annotation.Resource;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Resource
    private MemberFeignService memberFeignService;
    @Resource
    private CartFeignService cartFeignService;
    @Resource
    private ThreadPoolExecutor threadPoolExecutor;
    @Resource
    private WmsFeignService wmsFeignService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public OrderConfirmVo confirmOrder() throws ExecutionException, InterruptedException {
        OrderConfirmVo orderConfirmVo = new OrderConfirmVo();
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        // 启用多线程，获取主线程请求头数据，给每个线程保存一份，方便异步调用时有用户登陆信息。
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        CompletableFuture<Void> getAddress = CompletableFuture.runAsync(() -> {
            // 远程查询所有的收货地址
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<MemberAddressVo> address = memberFeignService.getAddress(memberRespVo.getId());
            orderConfirmVo.setAddress(address);
        }, threadPoolExecutor);

        CompletableFuture<Void> getCart = CompletableFuture.runAsync(() -> {
            // 远程查询购物车内的购物项
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> userItems = cartFeignService.getUserItems();
            orderConfirmVo.setItems(userItems);
        }, threadPoolExecutor).thenRunAsync(()->{
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            List<SkuHasStockVo> skusHasStock = wmsFeignService.getSkusHasStock(skuIds);
            if (skusHasStock!=null){
                Map<Long, Boolean> collect = skusHasStock.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                orderConfirmVo.setStocks(collect);
            }
        },threadPoolExecutor);

        CompletableFuture<Void> getOther = CompletableFuture.runAsync(() -> {
            // 远程查询用户优惠券信息（此处直接使用了用户积分）
            RequestContextHolder.setRequestAttributes(requestAttributes);
            Integer integration = memberRespVo.getIntegration();
            if (integration==null){
                orderConfirmVo.setIntegration(0);
            }else {
                orderConfirmVo.setIntegration(integration);
            }

            // 封装价格
            orderConfirmVo.setTotal();
            orderConfirmVo.setPayPrice();
        }, threadPoolExecutor);
        // 等所有异步任务完成，返回对象
        CompletableFuture.allOf(getAddress, getCart, getOther).get();

        // TODO 防重令牌
        String uuid = UUID.randomUUID().toString().replace("-","");
        orderConfirmVo.setOrderToken(uuid);
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX+memberRespVo.getId(),uuid,30, TimeUnit.MINUTES);

        return orderConfirmVo;
    }

    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) {

        //创建订单，验令牌，验价格，锁库存


        return null;
    }

}