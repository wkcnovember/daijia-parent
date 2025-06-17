package com.atguigu.daijia.coupon.handle;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.vo.coupon.CouponInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CouponPreheatService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    // 优惠券Redis键前缀
    private static final String COUPON_KEY = "c:";
    // 库存分段
    private static final String STOCK_KEY = "c:s:";
    // 用户键
    private static final String USER_KEY = "u:m:";


    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime of = LocalDateTime.of(2026, 02, 04, 01, 10, 20, 20);
        System.out.println(ChronoUnit.SECONDS.between(now, of));
    }

    public Boolean preHeat(CouponInfoVo couponInfoVo) {
        Long couponId = couponInfoVo.getId();
        // 计算剩余存活时间（秒）
        long ttlSeconds =
                ChronoUnit.SECONDS.between(LocalDateTime.now(), couponInfoVo.getExpireTime());
        // 活动无效发布==>到期
        if (ttlSeconds < 0) {
            log.warn("优惠券[{}]预热失败: 已过期", couponId);

            throw new GuiguException(ResultCodeEnum.COUPON_PUBLISH_ERROR);
        }

        ttlSeconds = ttlSeconds + new Random().nextInt(10, 300);
        // 2. 库存分段存储（减少竞争）
        if (couponInfoVo.getPublishCount() > 0) {
            // 2.1 计算可用库存
            int availableStock = couponInfoVo.getPublishCount() - couponInfoVo.getReceiveCount();
            // 2.2 计算分段数量
            int segments = Math.max(1, (int) Math.ceil(availableStock / 100.0));
            couponInfoVo.setSegmentCount(segments);
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
        } else {
            couponInfoVo.setSegmentCount(0);
        }


        // 使用Fastjson将对象转为Map
        String jsonString = JSON.toJSONString(couponInfoVo);
        Map<String, String> couponMap = JSON.parseObject(
                jsonString,
                new TypeReference<>() {
                }
        );

        stringRedisTemplate.opsForHash().putAll(COUPON_KEY + couponId, couponMap);
        Boolean expire = stringRedisTemplate.expire(COUPON_KEY + couponId, ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.FALSE.equals(expire)) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }


        return Boolean.TRUE;
    }


}

