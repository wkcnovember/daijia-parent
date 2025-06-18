package com.atguigu.daijia.coupon.handle;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.model.vo.coupon.CouponInfoVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.ReturnType;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.stream.Collectors;

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

    @Resource(name = "preheatStock")
    private DefaultRedisScript<Long> preheatStock;

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

        // 管道推送
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 2. 库存分段存储（减少竞争）
            if (couponInfoVo.getPublishCount() > 0) {
                // 2.1 计算可用库存
                int availableStock = couponInfoVo.getPublishCount() - couponInfoVo.getReceiveCount();
                // 2.2 计算分段数量
                int segments = Math.max(1, (int) Math.ceil(availableStock / 100.0));
                int lastSegmentStock = (availableStock % 100 == 0) ? 100 : (availableStock % 100);
                couponInfoVo.setSegmentCount(segments);
                connection.commands().eval(
                        preheatStock.getScriptAsString().getBytes(),
                        ReturnType.INTEGER,
                        1,
                        (STOCK_KEY + couponId).getBytes(),
                        String.valueOf(segments).getBytes(),
                        String.valueOf(ttlSeconds).getBytes(),
                        String.valueOf(lastSegmentStock).getBytes(),
                        String.valueOf(System.currentTimeMillis()).getBytes() // 随机种子
                );
            } else {
                couponInfoVo.setSegmentCount(0);
            }

            // 2.2 存储优惠券信息
            Map<byte[], byte[]> couponMap = JSON.parseObject(
                            JSON.toJSONString(couponInfoVo),
                            new TypeReference<Map<String, String>>() {
                            }
                    ).entrySet().stream()
                    .collect(Collectors.toMap(
                            e -> e.getKey().getBytes(),
                            e -> e.getValue().getBytes()
                    ));
            connection.commands().hMSet((COUPON_KEY + couponId).getBytes(), couponMap);
            connection.commands().expire((COUPON_KEY + couponId).getBytes(), ttlSeconds);

            return null;
        });


        return Boolean.TRUE;
    }


}

