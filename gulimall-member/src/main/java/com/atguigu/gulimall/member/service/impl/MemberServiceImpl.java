package com.atguigu.gulimall.member.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.common.utils.R;
import com.atguigu.gulimall.member.dao.MemberLevelDao;
import com.atguigu.gulimall.member.entity.MemberLevelEntity;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;

import com.atguigu.gulimall.member.vo.SocialUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;

import com.atguigu.gulimall.member.dao.MemberDao;
import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import org.springframework.web.client.RestTemplate;


import javax.annotation.Resource;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Resource
    private MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public void regist(MemberRegistVo vo) {

        MemberEntity entity = new MemberEntity();
        MemberDao memberDao = this.baseMapper;
        // 设置默认等级
        MemberLevelEntity levelEntity = memberLevelDao.getDefaultLevel();
        entity.setLevelId(levelEntity.getId());

        // 用户名手机号是否唯一 (个人认为应该在注册填表单时就验证而不是现在，此时应该验证通过状态，直接提交就好)
        entity.setMobile(vo.getPhone());
        entity.setUsername(vo.getUserName());
        entity.setNickname(vo.getUserName());
/*        // 密码使用 MD5 加密
        String pwd = DigestUtils.md5Hex(vo.getPassWord());
        // 盐值加密 随机值
        Md5Crypt.md5Crypt("123456".getBytes(StandardCharsets.UTF_8),"$123456")*/
        // spring 带的密码加密器
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String pwd = passwordEncoder.encode(vo.getPassWord());
        /*
        验证时使用如下
        boolean matches = passwordEncoder.matches("", "");
        */
        entity.setPassword(pwd);

        memberDao.insert(entity);
    }

    @Override
    public boolean checkPhoneUnique(String phone) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity member = memberDao.selectCheckPhone(phone);
        return member == null ? true : false;
    }

    @Override
    public boolean checkUserNameUnique(String userName) {
        MemberDao memberDao = this.baseMapper;
        MemberEntity member = memberDao.selectCheckUserName(userName);
        return member == null ? true : false;
    }

    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();

        // 数据库查询密码
        MemberDao memberDao = this.baseMapper;
        MemberEntity member = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("mobile", loginacct));
        if (member == null) {
            return null;
        } else {
            // 验证密码
            String passwordDb = member.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            if (passwordEncoder.matches(password, passwordDb)) {
                // 通过验证
                return member;
            } else {
                // 密码验证失败
                return null;
            }
        }
    }

    @Override
    public MemberEntity login(SocialUser socialUser) {
        // 登陆和注册合并逻辑
        String uid = socialUser.getUid();
        // 判断当前社交用户是否已经登陆过系统
        MemberDao memberDao = this.baseMapper;
        MemberEntity memberEntity = memberDao.selectOne(new QueryWrapper<MemberEntity>().eq("social_uid", uid));
        if (memberEntity != null) {
            // 用户已注册过
            MemberEntity member = new MemberEntity();
            member.setId(memberEntity.getId());
            member.setAccessToken(socialUser.getAccess_token());
            member.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            memberDao.updateById(member);

            memberEntity.setAccessToken(socialUser.getAccess_token());
            memberEntity.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            return memberEntity;
        } else {
            // 没有查到当前社交用户对应的记录，需注册一个
            MemberEntity regist = new MemberEntity();
            // 查询当前社交用户的社交账号信息（昵称，性别等）
            RestTemplate restTemplate = new RestTemplate();
            String url = "https://api.weibo.com/2/users/show.json?access_token=" + socialUser.getAccess_token() + "&uid=" + socialUser.getUid();
            try {
                ResponseEntity<String> forEntity = restTemplate.getForEntity(url, String.class, new HashMap<>());
                if (forEntity.getStatusCode().value() == 200) {
                    JSONObject jsonObject = JSON.parseObject(String.valueOf(forEntity.getBody()));
                    regist.setNickname(String.valueOf(jsonObject.get("name")));
                    regist.setGender("m".equals(String.valueOf(jsonObject.get("gender"))) ? 1 : 0);
                    regist.setHeader(String.valueOf(jsonObject.get("profile_image_url")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            regist.setSocialUid(socialUser.getUid());
            regist.setAccessToken(socialUser.getAccess_token());
            regist.setExpiresIn(String.valueOf(socialUser.getExpires_in()));
            memberDao.insert(regist);
            return regist;
        }
    }
}