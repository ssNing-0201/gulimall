package com.atguigu.gulimall.product.feign.fallback;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.product.feign.SeckillFeginSevice;
import org.springframework.stereotype.Component;

@Component
public class SeckillFeignServiceFallback implements SeckillFeginSevice {

    @Override
    public R getSkuSeckillInfo(Long skuId) {

        return R.error(BizCodeEnum.UNKNOW_EXCEPTION.getCode(),BizCodeEnum.UNKNOW_EXCEPTION.getMsg());
    }
}
