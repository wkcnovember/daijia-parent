package com.atguigu.daijia.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * @Author 柯佳元
 * @Create 2025/3/27 12:38
 * @Version 1.0
 * Description:
 */

@Configuration
public class LuaConfig {

    @Bean(name = "delDriverOrders")
    public DefaultRedisScript<Long> delDriverOrders() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/del_order.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }

    @Bean(name = "orderIdSuitableDriverIds")
    public DefaultRedisScript<Long> orderIdSuitableDriverIds() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/orderIdSuitableDriverIds.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }


}
