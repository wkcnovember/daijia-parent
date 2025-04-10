package com.atguigu.daijia.coupon.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.atguigu.daijia.common.constant.RedisConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.coupon.handle.CouponPreheatService;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.convert.coupon.CouponInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.atguigu.daijia.model.enums.coupon.CouponStatus;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.CouponInfoVo;
import com.atguigu.daijia.model.vo.coupon.NoReceiveCouponVo;
import com.atguigu.daijia.model.vo.coupon.NoUseCouponVo;
import com.atguigu.daijia.model.vo.coupon.UsedCouponVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


    @Resource
    private CustomerCouponMapper customerCouponMapper;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private CouponInfoConvert couponInfoConvert;

    @Resource
    private CouponPreheatService couponPreheatService;


    @Override
    public PageVo<NoReceiveCouponVo> findNoReceivePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoReceiveCouponVo> pageInfo = baseMapper.findNoReceivePage(pageParam, customerId);
        return PageVo.toPageVo(pageInfo);
    }

    @Override
    public PageVo<NoUseCouponVo> findNoUsePage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<NoUseCouponVo> pageInfo = baseMapper.findNoUsePage(pageParam, customerId);
        return PageVo.toPageVo(pageInfo);
    }

    @Override
    public PageVo<UsedCouponVo> findUsedPage(Page<CouponInfo> pageParam, Long customerId) {
        IPage<UsedCouponVo> pageInfo = baseMapper.findUsedPage(pageParam, customerId);
        return PageVo.toPageVo(pageInfo);
    }

    @Override
    @Transactional
    public Boolean receive(Long customerId, Long couponId) {
        // 1.查询 优惠券
        CouponInfo couponInfo = getById(couponId);
        if (couponId == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 2.优惠券过期日期判断
        if (couponInfo.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
        }
        // 3、校验库存，优惠券领取数量判断
        if (couponInfo.getPublishCount() != 0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
            throw new GuiguException(ResultCodeEnum.COUPON_LESS);
        }

        RLock lock = null;
        try {
            // 初始化分布式锁
            // 每人领取限制  与 优惠券发行总数 必须保证原子性，使用couponId减少锁的粒度，增加并发能力
            lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + couponId);
            boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME,
                    TimeUnit.SECONDS);
            if (flag) {
                // 4、校验每人限领数量
                if (couponInfo.getPerLimit() > 0) {
                    // 4.1、统计当前用户对当前优惠券的已经领取的数量
                    long count =
                            customerCouponMapper.selectCount(new LambdaQueryWrapper<CustomerCoupon>().eq(CustomerCoupon::getCouponId, couponId).eq(CustomerCoupon::getCustomerId, customerId));
                    // 4.2、校验限领数量
                    if (count >= couponInfo.getPerLimit()) {
                        throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
                    }
                }

                // 5、更新优惠券领取数量
                int row = 0;
                if (couponInfo.getPublishCount() == 0) {// 没有限制
                    row = baseMapper.updateReceiveCount(couponId);
                } else {
                    row = baseMapper.updateReceiveCountByLimit(couponId);
                }
                if (row == 1) {
                    // 6、保存领取记录
                    this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
                    return Boolean.TRUE;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
        throw new GuiguException(ResultCodeEnum.COUPON_LESS);
    }

    @Transactional
    @Override
    public Boolean publish(Long couponId) {

        CouponInfo couponInfo = getById(couponId);
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        LambdaUpdateWrapper<CouponInfo> updateCouponInfo = new LambdaUpdateWrapper<CouponInfo>()
                .set(CouponInfo::getStatus, CouponStatus.PUBLISHED.getStatus())
                .eq(BaseEntity::getId, couponId);

        boolean isSuccess = update(updateCouponInfo);
        if (!isSuccess) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        // 预热到缓存里~
        CouponInfoVo couponInfoVo = couponInfoConvert.toCouponInfoVo(couponInfo);
        // 1. 使用Fastjson将对象转为Map
        String jsonString = JSON.toJSONString(couponInfoVo);
        Map<String, String> couponMap = JSON.parseObject(
                jsonString,
                new TypeReference<>() {
                }
        );
        Boolean aBoolean = couponPreheatService.preHeat(couponInfoVo);
        if (Boolean.FALSE.equals(aBoolean)) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }


    private void saveCustomerCoupon(Long customerId, Long couponId, LocalDateTime expireTime) {
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setCouponId(couponId);
        customerCoupon.setStatus(1);
        customerCoupon.setReceiveTime(new Date());
        customerCoupon.setExpireTime(expireTime);
        customerCouponMapper.insert(customerCoupon);
    }
}
