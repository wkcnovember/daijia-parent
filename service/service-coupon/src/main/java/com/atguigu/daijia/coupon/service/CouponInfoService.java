package com.atguigu.daijia.coupon.service;

import com.atguigu.daijia.coupon.config.LuaResult;
import com.atguigu.daijia.model.dto.coupon.CouponInfoDto;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.AvailableCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface CouponInfoService extends IService<CouponInfo> {


    PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId);

    PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId);

    LuaResult receive(Long customerId, Long couponId);

    Boolean publish(Long couponId);

    List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount);

    BigDecimal useCoupon(UseCouponForm useCouponForm);

    void handleCouponInfoDto(CouponInfoDto couponInfoDto);
}
