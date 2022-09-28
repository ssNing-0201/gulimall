package com.atguigu.gulimall.member.service;

import com.atguigu.gulimall.member.vo.MemberRegistVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.gulimall.member.entity.MemberEntity;
import org.apache.ibatis.annotations.Param;

import java.util.Map;

/**
 * ??Ա
 *
 * @author Ning
 * @email sszhangningwowow@gmail.com
 * @date 2022-07-18 20:38:12
 */
public interface MemberService extends IService<MemberEntity> {

    PageUtils queryPage(Map<String, Object> params);

    void regist(MemberRegistVo vo);

    boolean checkPhoneUnique(@Param("phone") String phone);

    boolean checkUserNameUnique(@Param("userName") String userName);
}

