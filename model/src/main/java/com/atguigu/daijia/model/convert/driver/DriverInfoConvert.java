package com.atguigu.daijia.model.convert.driver;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/3/18 9:41
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface DriverInfoConvert {

    DriverLoginVo toDriverLoginVo(DriverInfo driverInfo);
}
