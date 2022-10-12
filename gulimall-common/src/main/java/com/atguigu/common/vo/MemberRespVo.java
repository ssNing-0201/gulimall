package com.atguigu.common.vo;

import lombok.Data;
import lombok.ToString;

import java.io.Serializable;

@ToString
@Data
public class MemberRespVo implements Serializable {

    private Long id;
    private Long levelId; // 会员等级 id
    private String username; // 用户名
    private String password; // 密码
    private String nickname; // 昵称
    private String mobile; // 手机
    private String email; // 邮箱
    private String header; // 头像
    private String gender; // 性别
    private String socialUid;
    private String accessToken;
    private String expiresIn;
    private Integer integration; // 积分

}
