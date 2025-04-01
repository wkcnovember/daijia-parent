package com.atguigu.daijia.model.query.order;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Author 柯佳元
 * @Create 2025/3/31 12:21
 * @Version 1.0
 * Description:
 */

@Data
@Schema(name = "司机在某时间段的订单数量")
public class OrderCount {

    @Schema(description = "司机Id")
    @NotNull
    @Positive
    private Long driverId;


    @Schema(description = "开始时间")
    @NotNull
    private LocalDateTime startServiceTime;

    @Schema(description = "结束时间")
    @NotNull
    private LocalDateTime endServiceTime;
}
