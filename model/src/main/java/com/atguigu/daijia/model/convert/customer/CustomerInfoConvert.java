package com.atguigu.daijia.model.convert.customer;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.vo.customer.CustomerInfoVo;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @Author 柯佳元
 * @Create 2025/3/17 12:35
 * @Version 1.0
 * Description:
 */
// 2. 具体映射器继承全局配置
@Mapper(config = GlobalMapperConfig.class)
public interface CustomerInfoConvert {

    // 自动继承componentModel、unmapped策略及uses依赖
    CustomerLoginVo toVo(CustomerInfo customerInfo);
}

