package com.atguigu.gulimall.seckill.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.atguigu.common.to.mq.SeckillOrderTo;
import com.atguigu.common.utils.R;
import com.atguigu.common.vo.MemberRespVo;
import com.atguigu.gulimall.seckill.feign.CouponFeginService;
import com.atguigu.gulimall.seckill.feign.ProductFeginService;
import com.atguigu.gulimall.seckill.interceptor.LoginUserInterceptor;
import com.atguigu.gulimall.seckill.service.SeckillService;
import com.atguigu.gulimall.seckill.to.SeckillSkuRedisTo;
import com.atguigu.gulimall.seckill.vo.SeckillSessionsWithSkus;
import com.atguigu.gulimall.seckill.vo.SeckillSkuVo;
import com.atguigu.gulimall.seckill.vo.SkuInfoVo;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.mysql.cj.util.StringUtils;
import org.redisson.api.RSemaphore;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class SeckillServiceImpl implements SeckillService {

    @Resource
    private CouponFeginService couponFeginService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ProductFeginService productFeginService;
    @Resource
    private RedissonClient redissonClient;
    @Resource
    private RabbitTemplate rabbitTemplate;

    private final String SESSIONS_CACHE_PREFIX = "seckill:sessions:";

    private final String SKUKILL_CHCHE_PREFIX = "seckill:skus";

    private final String SKU_STOCK_SEMAPHORE = "seckill:stock:";

    @Override
    public void uploadSeckillSkulatest3Days() {
        // 扫描需要参与秒杀活动
        R r = couponFeginService.getLates3DaySession();
        if (r.get("code").toString().equals("0")) {
            String s = JSON.toJSONString(r.get("data"));
            List<SeckillSessionsWithSkus> seckillSessionsWithSkuses = JSON.parseArray(s, SeckillSessionsWithSkus.class);
            if (seckillSessionsWithSkuses!=null){
                // 缓存活动信息
                saveSessionInfos(seckillSessionsWithSkuses);
                // 缓存活动关联商品信息
                saveSessionSkuInfos(seckillSessionsWithSkuses);
            }
        }
    }

    // 查询当前时间段可以参与秒杀的商品信息
    @Override
    public List<SeckillSkuRedisTo> getCurrentSeckillSkus() {
        // 确定当前时间属于哪个秒杀场次
        long time = new Date().getTime();
        Set<String> keys = stringRedisTemplate.keys(SESSIONS_CACHE_PREFIX + "*");
        for (String key : keys) {
            String replace = key.replace(SESSIONS_CACHE_PREFIX, "");
            String[] s = replace.split("_");
            Long startTime = Long.parseLong(s[0]);
            Long endTime = Long.parseLong(s[1]);
            if (time >= startTime && time <= endTime) {
                List<String> range = stringRedisTemplate.opsForList().range(key, -100, 100);
                BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CHCHE_PREFIX);
                List<String> list = hashOps.multiGet(range);
                if (list != null) {
                    List<SeckillSkuRedisTo> collect = list.stream().map(item -> {
                        SeckillSkuRedisTo redisTo = JSON.parseObject(item.toString(), SeckillSkuRedisTo.class);
                        return redisTo;
                    }).collect(Collectors.toList());
                    return collect;
                } else {
                    break;
                }
            }
        }
        // 获取秒杀场次所有商品信息
        return null;
    }

    @Override
    public SeckillSkuRedisTo getSkuSeckillInfo(Long skuId) {
        // 找到所有需要参与秒杀的key

        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CHCHE_PREFIX);
        Set<String> keys = hashOps.keys();
        if (keys != null && keys.size() > 0) {
            String regx = "\\d_" + skuId;
            for (String key : keys) {
                if (Pattern.matches(regx, key)) {
                    String s = hashOps.get(key);
                    SeckillSkuRedisTo redisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
                    redisTo.setRandomCode(null);
                    return redisTo;
                }
            }
        }
        return null;
    }

    @Override
    public String kill(String killId, String key, Integer num) {
        MemberRespVo memberRespVo = LoginUserInterceptor.loginUser.get();
        // 获取秒杀商品详细信息

        BoundHashOperations<String, String, String> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CHCHE_PREFIX);
        String s = hashOps.get(killId);
        if (!StringUtils.isNullOrEmpty(s)) {

            SeckillSkuRedisTo redisTo = JSON.parseObject(s, SeckillSkuRedisTo.class);
            // 校验合法性
            Long endTime = redisTo.getEndTime();
            Long startTime = redisTo.getStartTime();
            long nowTime = new Date().getTime();
            Long ttl = endTime - nowTime;
            String code = redisTo.getRandomCode();
            Integer seckillLimit = redisTo.getSeckillLimit();
            // 校验合法性
            if (nowTime >= startTime && nowTime <= endTime && code.equals(key) && num <= seckillLimit) {
                String redisKey = memberRespVo.getId() + "_" + killId;
                // 超时时间设置(自动过期)
                Boolean aBoolean = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, num.toString(), ttl, TimeUnit.MILLISECONDS);
                if (aBoolean) {
                    // 占位成功说明从来没买过
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + code);
                    try {
                        semaphore.tryAcquire(num,100,TimeUnit.MILLISECONDS);
                        // 秒杀成功 快速下单
                        String orderSn = IdWorker.getTimeId();
                        SeckillOrderTo orderTo = new SeckillOrderTo();
                        orderTo.setOrderSn(orderSn);
                        orderTo.setMemberId(memberRespVo.getId());
                        orderTo.setNum(num);
                        orderTo.setPromotionSessionId(redisTo.getPromotionSessionId());
                        orderTo.setSkuId(redisTo.getSkuId());
                        orderTo.setSeckillPrice(redisTo.getSeckillPrice());
                        rabbitTemplate.convertAndSend("order-event-exchange","order.seckill.order",orderTo);
                        return orderSn;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        return null;
                    }
                } else {
                    // 占位失败说明已经买过了
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }


    private void saveSessionInfos(List<SeckillSessionsWithSkus> sessionsWithSkus) {
        sessionsWithSkus.stream().forEach(session -> {
            long startTime = session.getStartTime().getTime();
            long endTime = session.getEndTime().getTime();
            String key = SESSIONS_CACHE_PREFIX + startTime + "_" + endTime;
            // 缓存活动信息
            Boolean hasKey = stringRedisTemplate.hasKey(key);
            if (!hasKey) {
                List<String> collect = session.getRelationSkus().stream().map(item -> item.getPromotionSessionId().toString() + "_" + item.getSkuId().toString()).collect(Collectors.toList());
                stringRedisTemplate.opsForList().leftPushAll(key, collect);
                // 设置过期时间为一天
                stringRedisTemplate.opsForList().getOperations().expire(key,1,TimeUnit.DAYS);
            }

        });
    }

    private void saveSessionSkuInfos(List<SeckillSessionsWithSkus> sessionsWithSkus) {

        sessionsWithSkus.stream().forEach(session -> {

            // 准备哈希操作
            BoundHashOperations<String, Object, Object> hashOps = stringRedisTemplate.boundHashOps(SKUKILL_CHCHE_PREFIX);
            session.getRelationSkus().stream().forEach(seckillSkuVo -> {
                String token = UUID.randomUUID().toString().replace("-", "");
                String key = seckillSkuVo.getPromotionSessionId().toString() + "_" + seckillSkuVo.getSkuId().toString();
                if (!hashOps.hasKey(key)) {
                    // 缓存商品
                    SeckillSkuRedisTo redisTo = new SeckillSkuRedisTo();
                    // 1、sku基本信息
                    R r = productFeginService.getSkuInfo(seckillSkuVo.getSkuId());
                    if (r.get("code").toString().equals("0")) {
                        String skuInfo = JSON.toJSONString(r.get("skuInfo"));
                        SkuInfoVo skuInfoVo = JSON.parseObject(skuInfo, SkuInfoVo.class);
                        redisTo.setSkuInfo(skuInfoVo);
                    }
                    // 2、sku的秒杀信息
                    BeanUtils.copyProperties(seckillSkuVo, redisTo);
                    // 3、设置当前商品秒杀时间信息
                    redisTo.setStartTime(session.getStartTime().getTime());
                    redisTo.setEndTime(session.getEndTime().getTime());
                    // 4、设置商品随机码
                    redisTo.setRandomCode(token);
                    // 引入分布式信号量 限流
                    String s = JSON.toJSONString(redisTo);
                    hashOps.put(key, s);
                    // 设置过期时间为一天
                    hashOps.getOperations().expire(key,1,TimeUnit.DAYS);
                    // 如果当前场次库存信息已经上架就不需要上架
                    // 使用库存作为分布式信号量 限流
                    RSemaphore semaphore = redissonClient.getSemaphore(SKU_STOCK_SEMAPHORE + token);
                    // 商品可以秒杀的数量(库存)作为信号量
                    semaphore.trySetPermits(seckillSkuVo.getSeckillCount());
                }
            });
        });
    }
}
