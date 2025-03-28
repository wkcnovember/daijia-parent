package com.atguigu.daijia.dispatch.config;

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
    @Bean(name = "addDriverOrders")
    public DefaultRedisScript<Long> addDriverOrders() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/driver_recent_order.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }

    /**
     * 删除司机订单列表中不符合的订单
     * @return
     */
    @Bean(name = "delDriverOrderKeys")
    public DefaultRedisScript<Long> delDriverOrderKeys() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/del_order_key.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }

    /**
     * 删除司机的订单
     * @return
     */

    @Bean(name = "delDriverOrders")
    public DefaultRedisScript<Long> delDriverOrders() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(
                new ClassPathResource("lua/del_orders.lua")));
        script.setResultType(Long.class); // 返回值类型
        return script;
    }
}
