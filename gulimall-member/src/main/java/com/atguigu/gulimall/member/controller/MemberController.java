package com.atguigu.gulimall.member.controller;

import java.util.Arrays;
import java.util.Map;

import com.atguigu.common.exception.BizCodeEnum;
import com.atguigu.gulimall.member.feign.CouponFeignService;
import com.atguigu.gulimall.member.vo.MemberLoginVo;
import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.atguigu.gulimall.member.vo.SocialUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gulimall.member.entity.MemberEntity;
import com.atguigu.gulimall.member.service.MemberService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.R;

import javax.annotation.Resource;


/**
 * ??Ա
 *
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 20:38:12
 */
@RestController
@RequestMapping("member/member")
public class MemberController {
    @Autowired
    private MemberService memberService;

    @Resource
    private CouponFeignService couponFeignService;


    @PostMapping("/oauth2/login")
    public R oauthLogin(@RequestBody SocialUser socialUser){
        MemberEntity entity = memberService.login(socialUser);
        if (entity!=null){
            // 登陆成功
            return R.ok().put("data",entity);
        }else {
            return R.error(BizCodeEnum.LOGINACCT_PWD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PWD_EXCEPTION.getMsg());
        }
    }

    @PostMapping("/login")
    public R login(@RequestBody MemberLoginVo vo){
        MemberEntity member = memberService.login(vo);
        if (member == null){
            return R.error(BizCodeEnum.LOGINACCT_PWD_EXCEPTION.getCode(), BizCodeEnum.LOGINACCT_PWD_EXCEPTION.getMsg());
        }else {
            // TODO 登陆成功处理
            return R.ok().put("data",member);
        }

    }

    @PostMapping("/regist")
    public R regist(@RequestBody MemberRegistVo vo){

        memberService.regist(vo);

        return R.ok();
    }
    @PostMapping("/checkphone")
    public boolean checkphone(@RequestBody String phone){
        return memberService.checkPhoneUnique(phone);
    }
    @PostMapping("/checkusername")
    public boolean checkusername(@RequestBody String userName){
        return memberService.checkUserNameUnique(userName);
    }

    @RequestMapping("/coupons")
    public R test(){
        MemberEntity memberEntity = new MemberEntity();
        memberEntity.setNickname("张三");
        R memberCoupons = couponFeignService.memberCoupons();
        return R.ok().put("member",memberEntity).put("coupons",memberCoupons.get("coupons"));
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("member:member:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = memberService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{id}")
    //@RequiresPermissions("member:member:info")
    public R info(@PathVariable("id") Long id){
		MemberEntity member = memberService.getById(id);

        return R.ok().put("member", member);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("member:member:save")
    public R save(@RequestBody MemberEntity member){
		memberService.save(member);

        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("member:member:update")
    public R update(@RequestBody MemberEntity member){
		memberService.updateById(member);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("member:member:delete")
    public R delete(@RequestBody Long[] ids){
		memberService.removeByIds(Arrays.asList(ids));

        return R.ok();
    }

}
