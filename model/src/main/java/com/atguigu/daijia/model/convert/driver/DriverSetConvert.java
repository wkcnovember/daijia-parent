package com.atguigu.daijia.model.convert.driver;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/3/22 9:56
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface DriverSetConvert {

    DriverSetVo toDriverSetVo(DriverSet driverSet);
}
