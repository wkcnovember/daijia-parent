package com.atguigu.daijia.common.auth;

import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.model.constants.auth.AuthConstants;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@Aspect  // 切面类
@Slf4j
public class GuiguLoginAspect {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 环绕通知，登录判断
    // 切入点表达式：指定对哪些规则的方法进行增强
    @Around("execution(* com.atguigu.daijia.*.controller.*.*(..)) && @annotation(kjyLogin)")
    public Object login(ProceedingJoinPoint proceedingJoinPoint, KjyLogin kjyLogin)  {

        // 1 获取request对象
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        ServletRequestAttributes sra = (ServletRequestAttributes) attributes;
        if (null == sra) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        HttpServletRequest request = sra.getRequest();


        // 2 从请求头获取token
        String token = request.getHeader(AuthConstants.TOKEN_NAME);

        // 3 判断token是否为空，如果为空，返回登录提示
        if (!StringUtils.hasText(token)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 4 token不为空，查询redis
        String customerId = stringRedisTemplate.opsForValue()
                .get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        if (!StringUtils.hasText(customerId)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }

        // 5 查询redis对应用户id，把用户id放到ThreadLocal里面
        if (StringUtils.hasText(customerId)) {
            AuthContextHolder.setUserId(Long.parseLong(customerId));
        }

        // 6 执行业务方法
        try {
            return proceedingJoinPoint.proceed();
        } catch (Throwable e) {
            log.error("切面报错=={}",e.getMessage());
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        } finally {
            log.debug("Cleaned ThreadLocal for thread: {}", Thread.currentThread().getName());
            AuthContextHolder.removeUserId();
        }

    }
}


