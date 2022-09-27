package com.atguigu.gulimall.auth.controller;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.auth.constant.SendCodeConstant;
import com.atguigu.gulimall.auth.feign.ThirdPartyFeignService;
import com.atguigu.gulimall.auth.vo.UserRegistVo;
import com.mysql.cj.util.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class LoginController {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ThirdPartyFeignService thirdPartyFeignService;

    /**
     * 注册方法
     * RedirectAttributes redirectAttributes 重定向，视图携带数据
     */
    @PostMapping("/regist")
    public String regist(@Valid UserRegistVo vo, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            Map<String, String> collect = result.getFieldErrors().stream().collect(Collectors.toMap(fieldError -> fieldError.getField(), fieldError -> fieldError.getDefaultMessage()));
            // 数据格式校验出错，返回注册页
//            model.addAttribute("error",collect);
            redirectAttributes.addFlashAttribute("errors",collect);
            return "redirect:http://auth.gulimall.com/reg.html";
        }
        // 注册
        // 1、校验验证码
        String code = vo.getCode();
        String s = stringRedisTemplate.opsForValue().get(SendCodeConstant.SMS_CODE_CHCHE_PREFIX + vo.getPhone());
        if (!StringUtils.isNullOrEmpty(s)){

        }
        // 注册成功跳转登陆页
        return "redirect:/login.html"; // 此处这么写因为config中映射了登陆页地址，所以可以省略 http://auth.gulimall.com
    }


    /**
     * 获取短信验证码的方法
     *
     * @param phone
     * @return
     * @throws Exception
     */
    @RequestMapping("/sms/sendcode")
    @ResponseBody
    public R sendCode(@RequestParam("phone") String phone) throws Exception {

        // TODO 接口防刷

        // 解决验证码60s内只能发送一次
        String s = stringRedisTemplate.opsForValue().get(SendCodeConstant.SMS_CODE_CHCHE_PREFIX + phone);
        if (StringUtils.isNullOrEmpty(s)) {
            toSendCode(phone);
        } else {
            long l = Long.parseLong(s.split("_")[1]);
            if (System.currentTimeMillis() - l < 60000) {
                return R.error(BizCodeEnum.SMS_CODE_EXCEPTION.getMsg());
            } else {
                toSendCode(phone);
            }
        }
        return R.ok();
    }

    private void toSendCode(String phone) throws Exception {
        String code = getCode(SendCodeConstant.CODE_BIT_NUMBER) + "_" + System.currentTimeMillis();
        // redis 缓存验证码。
        stringRedisTemplate.opsForValue().set(SendCodeConstant.SMS_CODE_CHCHE_PREFIX + phone, code, SendCodeConstant.CODE_EXPIRATION_TIME, TimeUnit.MINUTES);
        thirdPartyFeignService.sendCode(phone, code);
    }

    // 生成验证码， x 为验证码位数
    public String getCode(int x) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < x; i++) {
            int num = (int) (Math.random() * 10);
            builder.append(num);
        }
        return builder.toString();
    }

}
