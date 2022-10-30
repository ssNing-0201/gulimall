package com.atguigu.gulimall.seckill.interceptor;


import com.atguigu.common.constant.AuthServerContant;
import com.atguigu.common.vo.MemberRespVo;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginUserInterceptor implements HandlerInterceptor {

    public static ThreadLocal<MemberRespVo> loginUser = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String requestURI = request.getRequestURI();
        // 秒杀请求需要判断是否登陆，其余请求都可放行
        boolean match = new AntPathMatcher().match("/kill", requestURI);

        if (match){
            MemberRespVo attribute = (MemberRespVo) request.getSession().getAttribute(AuthServerContant.LOGIN_USER);
            if (attribute!=null){
                loginUser.set(attribute);
                return true;
            }else {
                request.getSession().setAttribute("msg","请先登陆");
                response.sendRedirect("http://auth.gulimall.com/login.html");
                return false;
            }
        }

        return true;
    }
}
