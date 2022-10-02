package com.atguigu.gulimall.auth.entity;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class SocialUser {

    private String access_token;
    private String remind_in;
    private long expires_in;
    private String uid;
    private String isRealName;
}
