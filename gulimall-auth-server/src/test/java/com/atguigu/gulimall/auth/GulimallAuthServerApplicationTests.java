package com.atguigu.gulimall.auth;

import com.atguigu.gulimall.auth.feign.CheckuserNameAndPhoneFeignService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class GulimallAuthServerApplicationTests {

    @Resource
    CheckuserNameAndPhoneFeignService feignService;


    @Test
    public void test(){
        boolean checkphone = feignService.checkphone("18650359347");
        System.out.println(checkphone);
    }
    @Test
    public void test01(){
        boolean checkusername = feignService.checkusername("zhangsan");
        System.out.println(checkusername);
    }

    @Test
    void contextLoads() {

    }
    @Test
    public void getCode() {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            int num = (int) (Math.random() * 10);
            builder.append(num);
        }
        System.out.println(builder.toString());
    }

}
