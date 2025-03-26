package com.atguigu.daijia.model.vo.driver;

import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @Author 柯佳元
 * @Create 2025/3/22 9:54
 * @Version 1.0
 * Description:
 */

@Data
public class DriverSetVo {

    @Schema(description = "司机ID")
    private Long driverId;

    @Schema(description = "服务状态 1：开始接单 0：未接单")
    private Integer serviceStatus;

    @Schema(description = "订单里程设置")
    private BigDecimal orderDistance;

    @Schema(description = "接单里程设置")
    private BigDecimal acceptDistance;

    @Schema(description = "是否自动接单")
    private Integer autoAccept;
}
