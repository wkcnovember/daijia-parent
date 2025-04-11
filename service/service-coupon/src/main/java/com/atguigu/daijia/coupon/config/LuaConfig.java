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
                new ClassPathResource("lua/coupon_seckill.lua")));
        script.setResultType(List.class); // 返回值类型
        return script;
    }

}
