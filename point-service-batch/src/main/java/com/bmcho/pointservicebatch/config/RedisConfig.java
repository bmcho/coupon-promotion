package com.bmcho.pointservicebatch.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;
    @Value("${spring.data.redis.port}")
    private String port;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        String address = "redis://" + host + ":" + port;
        Config config = new Config();
        config.useSingleServer()
                .setAddress(address);
        return Redisson.create();
    }

}