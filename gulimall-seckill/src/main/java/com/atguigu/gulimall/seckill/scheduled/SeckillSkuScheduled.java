package com.atguigu.gulimall.seckill.scheduled;

import com.atguigu.gulimall.seckill.service.SeckillService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;


/**
 * 秒杀商品定时上架
 *  每晚3点上架最近3天需秒杀商品。
 */
@Service
@Slf4j
public class SeckillSkuScheduled {

    @Resource
    private SeckillService seckillService;
    @Resource
    private RedissonClient redissonClient;

    private final String upload_lock = "seckill:upload:lock";
    // TODO 幂等性处理
    @Scheduled(cron = "0 * * * * ?")
    public void uploadSeckillSkulatest3Days(){
        // 重复上架无需处理
        log.info("上架秒杀商品信息--->");
        // 分布式锁
        RLock lock = redissonClient.getLock(upload_lock);
        lock.lock(10, TimeUnit.SECONDS);
        try{
            seckillService.uploadSeckillSkulatest3Days();
        }finally {
            lock.unlock();
        }

    }

}
