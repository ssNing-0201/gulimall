package com.atguigu.gulimall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class GulimaillCorsConfiguration {

    @Bean
    public CorsWebFilter corsWebFilter(){
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        CorsConfiguration corsConfiguration = new CorsConfiguration();
        // 1、配置跨域
        corsConfiguration.addAllowedHeader("*"); // 请求头
        corsConfiguration.addAllowedMethod("*"); // 请求方法
        // 下面这个跟老师的不同因为高版本要使用OriginPattern才行
        corsConfiguration.addAllowedOriginPattern("*"); // 来源
        corsConfiguration.setAllowCredentials(true); // 是否携带cookie

        source.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(source);
    }
}
