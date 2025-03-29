package com.atguigu.daijia.model.convert.driver;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * @Author 柯佳元
 * @Create 2025/3/18 9:41
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface DriverInfoConvert {

    DriverLoginVo toDriverLoginVo(DriverInfo driverInfo);

    @Mapping(target = "id", source = "driverId")
    DriverInfo toDriverInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm);

    @Mapping(target = "driverId", source = "id")
    DriverAuthInfoVo toDriverAuthInfoVo(DriverInfo driverInfo);

    DriverInfoVo toDriverInfoVo(DriverInfo driverInfo);
}
