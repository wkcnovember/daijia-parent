package com.atguigu.daijia.coupon.handle;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.vo.coupon.CouponInfoVo;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @Author 柯佳元
 * @Create 2025/4/9 9:25
 * @Version 1.0
 * Description:
 */
@Service
public class CouponPreheatService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 优惠券Redis键前缀
    private static final String COUPON_KEY = "c:";
    private static final String STOCK_KEY = "c:s:";
    // private static final String LIMIT_KEY = "c:l:";
    // private static final String PREHEAT_LOCK = "preheat:lock:";
    // private static final String PREHEAT_MARK = "preheat:mark:";


    public Boolean preHeat(CouponInfoVo couponInfoVo) {
        // 1. 使用Fastjson将对象转为Map
        String jsonString = JSON.toJSONString(couponInfoVo);
        Map<String, String> couponMap = JSON.parseObject(
                jsonString,
                new TypeReference<>() {
                }
        );
        Long couponId = couponInfoVo.getId();
        stringRedisTemplate.opsForHash().putAll(COUPON_KEY + couponId, couponMap);
        // 计算剩余存活时间（秒）

        long ttlSeconds =
                ChronoUnit.SECONDS.between(LocalDateTime.now(), couponInfoVo.getExpireTime()) + new Random().nextInt(300);
        if (ttlSeconds > 0) {
            Boolean expire = stringRedisTemplate.expire(COUPON_KEY + couponId, ttlSeconds, TimeUnit.SECONDS);
            if (Boolean.FALSE.equals(expire)) {
                throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
            }
        } else {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }


        // 2. 库存分段存储（减少竞争）
        if (couponInfoVo.getPublishCount() > 0) {
            // 2.1 计算可用库存
            int availableStock = couponInfoVo.getPublishCount() - couponInfoVo.getReceiveCount();
            // 2.2 计算分段数量
            int segments = Math.max(1, (int) Math.ceil(availableStock / 100.0));

            /**
             * 段库存计算：
             * 前n-1段：每段100个库存
             * 最后一段：剩余所有库存（可能少于100）
             */
            for (int i = 0; i < segments; i++) {
                int segmentStock = (i == segments - 1) ?
                        availableStock - i * 100 : 100;
                stringRedisTemplate.opsForValue().set(STOCK_KEY + couponId + ":" + i, String.valueOf(segmentStock),
                        ttlSeconds, TimeUnit.SECONDS);
            }
        }



        return Boolean.TRUE;
    }


}

