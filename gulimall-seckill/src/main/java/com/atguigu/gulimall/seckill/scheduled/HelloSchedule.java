package com.atguigu.gulimall.seckill.scheduled;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// @EnableScheduling // 开启定时任务
//@Component
//@Slf4j
// @EnableAsync // 开启异步任务
public class HelloSchedule {

    /**
     *  cron表达式：
     *  1、spring 中只能有6位组成，不允许第七位
     *  2、在周几的位置，1-7 代表周一到周天
     *  3、定时任务不应该阻塞，但，默认是阻塞的
     *    解决：
     *      1）业务运行在异步线程中 (自己创建异步任务)
     *      2）支持定时任务线程池 设置配置文件配置增加线程池数量 (容易失效)
     *      3）让定时任务异步执行 (添加注解)
     */
//    @Async // 给希望异步执行的方法标上该注解
//    @Scheduled(cron = "* * * * * ?")    // 开启定时任务
//    public void hello() throws InterruptedException {
//        log.info("hello!");
//        Thread.sleep(3000);
//    }
}
