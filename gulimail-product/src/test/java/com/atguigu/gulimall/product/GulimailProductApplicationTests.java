package com.atguigu.gulimall.product;

import com.atguigu.gulimall.product.entity.BrandEntity;
import com.atguigu.gulimall.product.service.BrandService;
import com.atguigu.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.UUID;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
class GulimailProductApplicationTests {

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    BrandService brandService;

    @Resource
    CategoryService categoryService;

    @Test
    public void testRedis(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        // 保存数据
        ops.set("hello","world_"+ UUID.randomUUID().toString());
        //查询
        String hello = ops.get("hello");

        System.out.println(hello);
    }



    @Test
    public void testFindPath(){
        Long[] catelogPath = categoryService.findCatelogPath(3L);
        log.info("完整路径{}", Arrays.asList(catelogPath));
    }

    @Test
    void contextLoads() {

        BrandEntity brandEntity = new BrandEntity();
        brandEntity.setName("华为");
        brandService.save(brandEntity);
        System.out.println("保存成功!");

    }

}
