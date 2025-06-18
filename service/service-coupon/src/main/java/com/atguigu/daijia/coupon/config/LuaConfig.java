package com.atguigu.daijia.coupon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

@Configuration
public class LuaConfig {

    @Bean(name = "couponSecKill")
    public DefaultRedisScript<List> couponSecKill() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/coupon_seckill1.lua")));
        script.setResultType(List.class); // 返回值类型
        return script;
    }

    @Bean(name = "preheatStock")
    public DefaultRedisScript<Long> preheatStock() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/preHeat_stock.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }

}
