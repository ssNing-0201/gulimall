package com.atguigu.gulimall.auth.feign;

import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.entity.SocialUser;
import com.atguigu.gulimall.auth.vo.MemberRegistVo;
import com.atguigu.gulimall.auth.vo.UserLoginVo;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "gulimall-member")
public interface MemberFeignService {

    @PostMapping("/member/member/regist")
    public R regist(@RequestBody MemberRegistVo vo);

    @PostMapping("/member/member/login")
    public R login(@RequestBody UserLoginVo vo);

    @PostMapping("/member/member/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser);
}
