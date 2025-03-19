package com.atguigu.daijia.common.anno;

import com.atguigu.daijia.common.thread.MyThreadConfig;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Documented
@Import({MyThreadConfig.class})
public @interface EnableMyThreadPoolConfig {
}
