package com.atguigu.gmall.config;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;


public class RedisUtils {

    private JedisPool jedisPool;

    public void initJedisPool(String host,int port,int timeOut,int database){
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        //设置最大核心数
        jedisPoolConfig.setMaxTotal(200);
        //是否开启阻塞队列
        jedisPoolConfig.setBlockWhenExhausted(true);
        //设置最大等待时间
        jedisPoolConfig.setMaxWaitMillis(10*1000);
        //设置最小剩余数
        jedisPoolConfig.setMinIdle(10);
        //设置当前用户获取到jedis时候自动检查jedis是否可用
        jedisPoolConfig.setTestOnBorrow(true);

        //创建jedis线程池
        jedisPool = new JedisPool(jedisPoolConfig,host,port,timeOut);
    }

    public Jedis getJedis(){
        Jedis jedis = jedisPool.getResource();
        return jedis;
    }

}
