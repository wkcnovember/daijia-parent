package com.atguigu.daijia.common.config.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * @Author 柯佳元
 * @Create 2025/3/26 17:43
 * @Version 1.0
 * Description:
 */
public class RedissonConfig {

    private final static String ADDRESS_PREFIX = "redis://";

    @Bean
    RedissonClient redissonSingle(@Autowired RedisProperties redisProperties) {
        Config config = new Config();

        String host = redisProperties.getHost();
        if (!StringUtils.hasText(host)) {
            throw new RuntimeException("host is  empty");
        }
        SingleServerConfig serverConfig = config.useSingleServer()
                .setAddress(ADDRESS_PREFIX + host + ":" + redisProperties.getPort())
                .setTimeout((int) redisProperties.getTimeout().getSeconds());

        int database = redisProperties.getDatabase();
        serverConfig.setDatabase(database);
        return Redisson.create(config);
    }

}
