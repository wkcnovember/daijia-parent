package com.atguigu.daijia.coupon.config;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class TimeUtils {

    /**
     * LocalDateTime 转 Unix 时间戳（秒级）
     */
    public static long toUnixTimestamp(LocalDateTime dateTime) {
        return dateTime.toEpochSecond(ZoneOffset.UTC);
    }

    /**
     * Unix 时间戳（秒级）转 LocalDateTime
     */
    public static LocalDateTime fromUnixTimestamp(long timestamp) {
        return LocalDateTime.ofEpochSecond(timestamp, 0, ZoneOffset.UTC);
    }

    // 测试
    public static void main(String[] args) {
        LocalDateTime now = LocalDateTime.now();
        long timestamp = toUnixTimestamp(now);
        LocalDateTime restored = fromUnixTimestamp(timestamp);

        System.out.println("原始时间: " + now);
        System.out.println("时间戳: " + timestamp);
        System.out.println("恢复后时间: " + restored);
        System.out.println("是否一致: " + now.equals(restored)); // 应输出 true
    }
}
