package com.atguigu.gulimall.order.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * 在seata 0.9 版本之后，seata提供了DataSource默认代理的功能，并且默认开启，不用在配置DataSourcesProxt中。
 * 在seata 1.5 版本，使用了nacos做配置中心，seata使用ymal配置文件，不用每个服务中都配置数据源代理。可以直接使用。
 */
@Configuration
public class MySeataConfig {

    /*@Resource
    private DataSourceProperties dataSourceProperties;

    @Bean
    public DataSource dataSource(DataSourceProperties propertise){
        HikariDataSource dataSource = propertise.initializeDataSourceBuilder().type(HikariDataSource.class).build();
        if (StringUtils.hasText(propertise.getName())){
            dataSource.setPoolName(propertise.getName());
        }
        return new DataSourceProxy(dataSource);
    }*/

}
