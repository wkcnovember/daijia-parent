package com.atguigu.daijia.coupon.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LuaResult {
    private Integer code;
    private String msg;
}
