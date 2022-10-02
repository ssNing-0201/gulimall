package com.atguigu.gulimall.auth.controller;

import com.atguigu.gulimall.auth.entity.SocialUser;
import org.apache.coyote.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Controller
// 社交登陆
public class Oath2Controller {
    @Value("${auth.weibo.id}")
    private String id;
    @Value("${auth.weibo.secret}")
    private String secret;
    @Value("${auth.weibo.uri}")
    private String uri;

    @GetMapping("/oauth2.0/weibo/success")
    public String weibo(@RequestParam("code") String code){
        // https://api.weibo.com/oauth2/access_token?client_id="+id+"&client_secret="+secret+"&grant_type=authorization_code&redirect_uri="+uri+"&code="+code
        String url = "https://api.weibo.com/oauth2/access_token?client_id="+id+"&client_secret="+secret+"&grant_type=authorization_code&redirect_uri="+uri+"&code="+code;
        // 根据code 换取Token
        RestTemplate restTemplate = new RestTemplate();
        Map<String,String> map = new HashMap<>();
        map.put("client_id",id);
        map.put("client_secret",secret);
        map.put("grant_type",id);
        map.put("client_id","authorization_code");
        map.put("redirect_uri",uri);
        map.put("code",code);
        ResponseEntity<SocialUser> responseEntity = restTemplate.postForEntity(url,null, SocialUser.class);
        if (responseEntity.getStatusCode().value()==200){
            // 获取到了accessToken
            // 为第一次登陆用户生成一个会员信息账号
        }else {
            return "redirect:http://auth.gulimall.com/login.html";
        }
        // 成功跳回首页
        return "redirect:http://gulimall.com";
    }
}
