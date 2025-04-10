package com.atguigu.daijia.customer.service.impl;

import com.atguigu.daijia.coupon.client.CouponFeignClient;
import com.atguigu.daijia.customer.service.CouponService;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CouponServiceImpl implements CouponService {

    @Resource
    private CouponFeignClient couponFeignClient;


    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Long customerId, Long page, Long limit) {
        return couponFeignClient
                .findNoReceivePage(customerId, page, limit)
                .throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Long customerId, Long page, Long limit) {
        return couponFeignClient
                .findNoUsePage(customerId, page, limit)
                .throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Long customerId, Long page, Long limit) {
        return couponFeignClient
                .findUsedPage(customerId, page, limit)
                .throwOnFailureOrDataIsNull().getData();
    }

    @Override
    public Boolean receive(Long customerId, Long couponId) {
        return couponFeignClient.receive(customerId, couponId).throwOnFailureOrDataIsNull().getData();
    }
}
