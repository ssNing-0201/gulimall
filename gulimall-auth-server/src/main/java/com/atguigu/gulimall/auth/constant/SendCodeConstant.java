package com.atguigu.gulimall.auth.constant;

public class SendCodeConstant {

    public static final int CODE_BIT_NUMBER = 5; // 验证码位数，4～6

    public static final String SMS_CODE_CHCHE_PREFIX = "sms:code:"; // redis 中存验证码时 key 的前缀

    public static final Long CODE_EXPIRATION_TIME = 10l; // redis 验证码过期时间。

}
