package com.atguigu.daijia.model.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * @Author 柯佳元
 * @Create 2025/4/9 10:30
 * @Version 1.0
 * Description:
 */

@Data
public class CouponInfoVo implements Serializable {
    private Long id;
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "优惠卷类型 1 现金券 2 折扣")
    private Integer couponType;

    @Schema(description = "优惠卷名字")
    private String name;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "折扣：取值[1 到 10]")
    private BigDecimal discount;

    @Schema(description = "使用门槛 0->没门槛")
    private BigDecimal conditionAmount;

    @Schema(description = "发行数量")
    private Integer publishCount;

    @Schema(description = "每人限领张数")
    private Integer perLimit;

    @Schema(description = "已使用数量")
    private Integer useCount;

    @Schema(description = "领取数量")
    private Integer receiveCount;

    @Schema(description = "活动开始时间")
    private LocalDateTime startTime;
    @Schema(description = "过期时间")
    private LocalDateTime expireTime;


    @Schema(description = "活动开始时间戳")
    private long startTimeStamp;
    @Schema(description = "过期时间戳")
    private long expireTimeStamp;


    @Schema(description = "优惠券描述")
    private String description;

    @Schema(description = "状态[0-未发布，1-已发布， -1-已过期]")
    private Integer status;


    @Schema(description = "分段数量")
    private Integer segmentCount;

    public long getStartTimeStamp() {
        return startTime.toEpochSecond(ZoneOffset.UTC);
    }


    public long getExpireTimeStamp() {
        return expireTime.toEpochSecond(ZoneOffset.UTC);
    }

}
