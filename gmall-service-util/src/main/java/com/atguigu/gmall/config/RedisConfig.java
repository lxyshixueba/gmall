package com.atguigu.gmall.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.redis.host:disabled}")
    private String host;
    @Value("${spring.redis.port:6379}")
    private int port;
    @Value("${spring.redis.timeout:10}")
    private int timeOut;
    @Value("${spring.redis.database:1}")
    private int database;

    //String host,int port,int timeOut,int database
    @Bean
    public RedisUtils getRedisUtils(){
        RedisUtils redisUtils = new RedisUtils();
        redisUtils.initJedisPool(host,port,timeOut,database);
        return redisUtils;
    }

}
