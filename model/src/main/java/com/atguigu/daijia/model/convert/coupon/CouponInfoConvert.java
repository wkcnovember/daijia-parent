package com.atguigu.daijia.model.convert.coupon;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.vo.coupon.CouponInfoVo;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/4/9 10:33
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface CouponInfoConvert {

    CouponInfoVo toCouponInfoVo(CouponInfo couponInfo);
}
