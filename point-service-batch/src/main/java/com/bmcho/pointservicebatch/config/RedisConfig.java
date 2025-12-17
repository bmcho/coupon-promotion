package com.bmcho.pointservicebatch.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        return Redisson.create(); //기본값 localhost, 6379
    }

}