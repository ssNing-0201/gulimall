package com.atguigu.gulimall.order.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.constant.OrderConstant;
import com.atguigu.common.exception.NoStockException;
import com.atguigu.common.to.mq.OrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.order.entity.OrderItemEntity;
import com.atguigu.gulimall.order.entity.PaymentInfoEntity;
import com.atguigu.gulimall.order.enume.OrderStatusEnum;
import com.atguigu.gulimall.order.feign.CartFeignService;
import com.atguigu.gulimall.order.feign.MemberFeignService;
import com.atguigu.gulimall.order.feign.ProductFeignService;
import com.atguigu.gulimall.order.feign.WmsFeignService;
import com.atguigu.gulimall.order.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.order.service.OrderItemService;
import com.atguigu.gulimall.order.service.PaymentInfoService;
import com.atguigu.gulimall.order.to.OrderCreateTo;
import com.atguigu.gulimall.order.vo.*;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.seata.spring.annotation.GlobalTransactional;
import org.jetbrains.annotations.NotNull;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
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
    @Resource
    private ProductFeignService productFeignService;
    @Resource
    private OrderItemService orderItemService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private PaymentInfoService paymentInfoService;

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
        }, threadPoolExecutor).thenRunAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            List<OrderItemVo> items = orderConfirmVo.getItems();
            List<Long> skuIds = items.stream().map(item -> item.getSkuId()).collect(Collectors.toList());
            List<SkuHasStockVo> skusHasStock = wmsFeignService.getSkusHasStock(skuIds);
            if (skusHasStock != null) {
                Map<Long, Boolean> collect = skusHasStock.stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, SkuHasStockVo::getHasStock));
                orderConfirmVo.setStocks(collect);
            }
        }, threadPoolExecutor);

        CompletableFuture<Void> getOther = CompletableFuture.runAsync(() -> {
            // 远程查询用户优惠券信息（此处直接使用了用户积分）
            RequestContextHolder.setRequestAttributes(requestAttributes);
            Integer integration = memberRespVo.getIntegration();
            if (integration == null) {
                orderConfirmVo.setIntegration(0);
            } else {
                orderConfirmVo.setIntegration(integration);
            }

            // 封装价格
            orderConfirmVo.setTotal();
            orderConfirmVo.setPayPrice();
        }, threadPoolExecutor);
        // 等所有异步任务完成，返回对象
        CompletableFuture.allOf(getAddress, getCart, getOther).get();

        // TODO 防重令牌
        String uuid = UUID.randomUUID().toString().replace("-", "");
        orderConfirmVo.setOrderToken(uuid);
        stringRedisTemplate.opsForValue().set(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId(), uuid, 30, TimeUnit.MINUTES);

        return orderConfirmVo;
    }

    @GlobalTransactional // 开启 seata 全局事物
    @Transactional
    @Override
    public SubmitOrderResponseVo submitOrder(OrderSubmitVo vo) throws ExecutionException, InterruptedException {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        SubmitOrderResponseVo responseVo = new SubmitOrderResponseVo();
        responseVo.setCode(0);
        // 创建订单，验令牌，验价格，锁库存
        // 验证令牌「令牌的对比和删除必须保证原子性」
        // 使用lua脚本 返回值 0 表示令牌失败，返回值 1 表示对比成功，删除成功
        String script = "if redis.call('get',KEYS[1])== ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        String orderToken = vo.getOrderToken();
        // 以下就是原子验证令牌和删除令牌
        Long result = stringRedisTemplate.execute(new DefaultRedisScript<Long>(script, Long.class), Arrays.asList(OrderConstant.USER_ORDER_TOKEN_PREFIX + memberRespVo.getId()), orderToken);
        if (result == 1l) {
            // 验证成功
            // 1、创建订单
            OrderCreateTo order = createOrder(vo);
            // 2、验价
            BigDecimal payAmount = order.getOrder().getPayAmount();
            BigDecimal payPrice = vo.getPayPrice();
            if (Math.abs(payAmount.subtract(payPrice).doubleValue()) < 0.01) {
                // 金额对比成功
                // 保存订单
                saveOrder(order);
                // 锁定库存
                WareSkuLockVo lockVo = new WareSkuLockVo();
                List<OrderItemVo> collect = order.getOrderItems().stream().map(item -> {
                    OrderItemVo orderItemVo = new OrderItemVo();
                    orderItemVo.setSkuId(item.getSkuId());
                    orderItemVo.setCount(item.getSkuQuantity());
                    orderItemVo.setTitle(item.getSkuName());
                    return orderItemVo;
                }).collect(Collectors.toList());
                lockVo.setOrderSn(order.getOrder().getOrderSn());
                lockVo.setLooks(collect);
                //TODO 远程锁库存
                R r = wmsFeignService.OrderLockStock(lockVo);
                if (r.get("code").toString().equals("0")) {
                    // 锁成功了
                    responseVo.setOrder(order.getOrder());
                    // TODO 订单创建成功，发送消息给MQ
                    rabbitTemplate.convertAndSend("order-event-exchange", "order.create.order",order.getOrder() );
                    return responseVo;
                } else {
                    // 锁失败了
                    throw new NoStockException((Long) r.get("error"));
                }
            } else {
                // 金额对比失败
                responseVo.setCode(2);
                return responseVo;
            }
        } else {
            // 验证失败
            responseVo.setCode(1);
            return responseVo;
        }
    }

    @Override
    public OrderEntity getOrderByOrderSn(String orderSn) {
        OrderEntity order_sn = this.getOne(new QueryWrapper<OrderEntity>().eq("order_sn", orderSn));
        return order_sn;
    }

    /**
     * 关闭超时订单
     */
    @Override
    public void closeOrder(OrderEntity entity) {
        // 关闭订单前先查询订单最新状态
        OrderEntity orderEntity = this.getById(entity.getId());
        if (orderEntity.getStatus() == OrderStatusEnum.CREATE_NEW.getCode()) {
            OrderEntity updateEntity = new OrderEntity();
            updateEntity.setId(orderEntity.getId());
            updateEntity.setStatus(OrderStatusEnum.CANCLED.getCode());
            this.updateById(updateEntity);
            // 发给 MQ 一个关闭订单的信息
            OrderTo orderTo = new OrderTo();
            BeanUtils.copyProperties(orderEntity,orderTo);
            rabbitTemplate.convertAndSend("order-event-exchange","order.release.other.#",orderTo);
        }


    }

    @Override
    public PayVo getOrderPay(String orderSn) {
        PayVo payVo = new PayVo();
        OrderEntity orderEntity = this.getOrderByOrderSn(orderSn);
        payVo.setSubject("消费总额");
        BigDecimal bigDecimal = orderEntity.getPayAmount().setScale(2, BigDecimal.ROUND_UP);
        payVo.setTotal_amount(bigDecimal.toString());
        payVo.setOut_trade_no(orderEntity.getOrderSn());
        return payVo;
    }

    @Override
    public PageUtils queryPageWithItem(Map<String, Object> params) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>().eq("member_id",memberRespVo.getId()).orderByDesc("create_time")
        );
        List<OrderEntity> orderSn = page.getRecords().stream().map(order -> {
            List<OrderItemEntity> order_sn = orderItemService.list(new QueryWrapper<OrderItemEntity>().eq("order_sn", order.getOrderSn()));
            order.setItemEntities(order_sn);
            return order;
        }).collect(Collectors.toList());

        page.setRecords(orderSn);
        return new PageUtils(page);
    }

    /**
     * 处理支付宝异步回调
     * @return 返回success支付宝将停止发送回调消息
     * @param vo
     */
    @Override
    public String handlePayResult(PayAsyncVo vo) {
        // 1、保存交易流水
        PaymentInfoEntity infoEntity = new PaymentInfoEntity();
        infoEntity.setAlipayTradeNo(vo.getTrade_no());
        infoEntity.setOrderSn(vo.getOut_trade_no());
        infoEntity.setPaymentStatus(vo.getTrade_status());
        infoEntity.setCallbackTime(vo.getNotify_time());
        paymentInfoService.save(infoEntity);
        // 2、修改订单状态信息
        if ("TRADE_SUCCESS".equals(vo.getTrade_status()) || "TRADE_FINISHED".equals(vo.getTrade_status())){
            // 支付成功
            String orderSn = vo.getOut_trade_no();
            this.baseMapper.updateOrderStatus(orderSn,OrderStatusEnum.PAYED.getCode());
        }
        return "success";
    }


    /**
     * 保存订单数据
     */
    private void saveOrder(OrderCreateTo order) {

        OrderEntity orderEntity = order.getOrder();
        orderEntity.setModifyTime(new Date());
        this.save(orderEntity);
        List<OrderItemEntity> orderItems = order.getOrderItems();
        orderItemService.saveBatch(orderItems);
    }

    private OrderCreateTo createOrder(OrderSubmitVo vo) throws ExecutionException, InterruptedException {
        OrderCreateTo createTo = new OrderCreateTo();

        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        // 启用多线程，获取主线程请求头数据，给每个线程保存一份，方便异步调用时有用户登陆信息。
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        // 生成订单号 (此处使用了mybatis plus 带的，不确定是否可以商用)
        String orderSn = IdWorker.getTimeId();
        // 获取收获地址信息
        CompletableFuture<Void> fareOrder = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            createTo.setOrder(buildOrder(vo, orderSn, memberRespVo));
        }, threadPoolExecutor);
        // 订单项
        CompletableFuture<Void> itemsOrder = CompletableFuture.runAsync(() -> {
            RequestContextHolder.setRequestAttributes(requestAttributes);
            createTo.setOrderItems(buildOrderItems(orderSn));
        }, threadPoolExecutor);
        CompletableFuture.allOf(fareOrder, itemsOrder).get();
        // 验价
        computePrice(createTo);
        return createTo;
    }

    private void computePrice(OrderCreateTo createTo) {
        List<OrderItemEntity> items = createTo.getOrderItems();
        BigDecimal total = new BigDecimal(0.00);
        BigDecimal couponAmount = new BigDecimal(0.00);
        BigDecimal integrationAmount = new BigDecimal(0.00);
        BigDecimal promotionAmount = new BigDecimal(0.00);
        Integer gift = 0;
        Integer growth = 0;
        // 订单总额
        for (OrderItemEntity item : items) {
            total = total.add(item.getRealAmount());
            couponAmount = couponAmount.add(item.getCouponAmount());
            integrationAmount = integrationAmount.add(item.getIntegrationAmount());
            promotionAmount = promotionAmount.add(item.getPromotionAmount());
            gift += item.getGiftGrowth();
            growth += item.getGiftIntegration();
        }
        // 订单总额
        createTo.getOrder().setTotalAmount(total);
        // 应付总额(+运费)
        createTo.getOrder().setPayAmount(total.add(createTo.getOrder().getFreightAmount()));
        //
        createTo.getOrder().setPromotionAmount(promotionAmount);
        createTo.getOrder().setIntegrationAmount(integrationAmount);
        createTo.getOrder().setCouponAmount(couponAmount);
        // 设置积分成长值
        createTo.getOrder().setGrowth(growth);
        createTo.getOrder().setIntegration(gift);

    }

    private OrderEntity buildOrder(OrderSubmitVo vo, String orderSn, MemberRespVo memberRespVo) {
        R fare = wmsFeignService.getFare(vo.getAddrId());
        OrderEntity entity = new OrderEntity();
        entity.setCreateTime(new Date());

        entity.setMemberId(memberRespVo.getId());
        entity.setOrderSn(orderSn);
        String s = JSON.toJSONString(fare.get("data"));
        FareVo fareVo = JSON.parseObject(s, FareVo.class);
        // 运费
        entity.setFreightAmount(fareVo.getFare());
        // 收件人信息
        entity.setReceiverCity(fareVo.getAddress().getCity());
        entity.setReceiverDetailAddress(fareVo.getAddress().getDetailAddress());
        entity.setReceiverName(fareVo.getAddress().getName());
        entity.setReceiverPhone(fareVo.getAddress().getPhone());
        entity.setReceiverProvince(fareVo.getAddress().getProvince());
        entity.setReceiverPostCode(fareVo.getAddress().getPostCode());
        entity.setReceiverRegion(fareVo.getAddress().getRegion());
        // 设置订单状态信息
        entity.setStatus(OrderStatusEnum.CREATE_NEW.getCode());
        entity.setAutoConfirmDay(7);

        return entity;
    }

    private List<OrderItemEntity> buildOrderItems(String orderSn) {
        List<OrderItemEntity> orderItems = new ArrayList<>();
        List<OrderItemVo> items = cartFeignService.getUserItems();
        if (items != null && items.size() > 0) {
            orderItems = items.stream().map(cartItem -> {
                OrderItemEntity itemEntity = new OrderItemEntity();
                try {
                    itemEntity = buildOrderItem(cartItem, orderSn);
                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                return itemEntity;
            }).collect(Collectors.toList());
        }
        return orderItems;
    }

    @NotNull
    private OrderItemEntity buildOrderItem(OrderItemVo item, String orderSn) throws ExecutionException, InterruptedException {
        OrderItemEntity itemEntity = new OrderItemEntity();
        // 订单信息
        itemEntity.setOrderSn(orderSn);
        // spu信息
        R spu = productFeignService.getSpuBySku(item.getSkuId());
        String s1 = JSON.toJSONString(spu.get("spu"));
        SpuInfoVo spuVo = JSON.parseObject(s1, SpuInfoVo.class);
        itemEntity.setSpuId(spuVo.getId());
        itemEntity.setSpuName(spuVo.getSpuName());
        itemEntity.setSpuBrand(spuVo.getBrandId().toString());
        itemEntity.setCategoryId(spuVo.getCatalogId());
//        CompletableFuture<Void> spuOrder = CompletableFuture.runAsync(() -> {
//
//        }, threadPoolExecutor);
        // sku信息
        itemEntity.setSkuId(item.getSkuId());
        itemEntity.setSkuName(item.getTitle());
        itemEntity.setSkuPic(item.getImage());
        itemEntity.setSkuPrice(item.getPrice());
        String s = StringUtils.collectionToDelimitedString(item.getSkuAttr(), ";");
        itemEntity.setSkuAttrsVals(s);
        itemEntity.setSkuQuantity(item.getCount());
        // 优惠信息(没做,用0填充)
        itemEntity.setPromotionAmount(new BigDecimal(0.00));
        itemEntity.setRealAmount(new BigDecimal(0.00));
        itemEntity.setCouponAmount(new BigDecimal(0.00));
        itemEntity.setIntegrationAmount(new BigDecimal(0.00));
        // 积分信息
        itemEntity.setGiftGrowth(item.getTotalPrice().intValue());
        itemEntity.setGiftIntegration(item.getTotalPrice().intValue());
//        CompletableFuture.allOf(spuOrder).get();
        //TODO 订单项价格信息(总价),没做优惠信息，所以这里算总价没计入优惠价格
        BigDecimal multiply = item.getPrice().multiply(new BigDecimal(item.getCount()));
        itemEntity.setRealAmount(multiply);
        return itemEntity;
    }
}