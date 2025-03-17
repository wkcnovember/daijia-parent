package com.atguigu.daijia.model.convert;

import ch.qos.logback.classic.pattern.DateConverter;
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

// 1. 创建全局配置类
@MapperConfig(
    componentModel = "spring",          // 使用Spring组件模型
    unmappedSourcePolicy = ReportingPolicy.IGNORE,  // 忽略未映射的源字段
    unmappedTargetPolicy = ReportingPolicy.WARN,    // 警告未映射的目标字段
    uses = {} // 公共依赖的转换器
)
public interface GlobalMapperConfig {}
