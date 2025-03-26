package com.atguigu.daijia.common.anno;

import com.atguigu.daijia.common.config.redis.RedissonConfig;
import com.atguigu.daijia.common.thread.MyThreadConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @Author 柯佳元
 * @Create 2025/3/26 17:43
 * @Version 1.0
 * Description:
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({RedissonConfig.class})
public @interface EnableRedisson {
}
