package com.atguigu.gulimall.auth.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "gulimall-member")
public interface CheckuserNameAndPhoneFeignService {

    @PostMapping("/member/member/checkphone")
    public boolean checkphone(@RequestBody String phone);

    @PostMapping("/member/member/checkusername")
    public boolean checkusername(@RequestBody String userName);

}
