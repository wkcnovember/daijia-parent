package com.atguigu.daijia.coupon.service.impl;

import com.alibaba.fastjson2.JSON;
import com.atguigu.daijia.common.constant.MqConst;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.service.RabbitService;
import com.atguigu.daijia.coupon.config.LuaResult;
import com.atguigu.daijia.coupon.config.TimeUtils;
import com.atguigu.daijia.coupon.handle.CouponPreheatService;
import com.atguigu.daijia.coupon.mapper.CouponInfoMapper;
import com.atguigu.daijia.coupon.mapper.CustomerCouponMapper;
import com.atguigu.daijia.coupon.service.CouponInfoService;
import com.atguigu.daijia.model.convert.coupon.CouponInfoConvert;
import com.atguigu.daijia.model.dto.coupon.CouponInfoDto;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.coupon.CouponInfo;
import com.atguigu.daijia.model.entity.coupon.CustomerCoupon;
import com.atguigu.daijia.model.enums.coupon.CouponStatus;
import com.atguigu.daijia.model.enums.coupon.CouponType;
import com.atguigu.daijia.model.enums.coupon.CustomerCouponStatus;
import com.atguigu.daijia.model.form.coupon.UseCouponForm;
import com.atguigu.daijia.model.vo.base.PageVo;
import com.atguigu.daijia.model.vo.coupon.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class CouponInfoServiceImpl extends ServiceImpl<CouponInfoMapper, CouponInfo> implements CouponInfoService {


    @Resource
    private CustomerCouponMapper customerCouponMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CouponInfoConvert couponInfoConvert;

    @Resource
    private CouponPreheatService couponPreheatService;


    @Resource
    private DefaultRedisScript<List> couponSecKill;

    @Resource
    private RabbitService rabbitService;


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
    public LuaResult receive(Long customerId, Long couponId) {
        List raw = stringRedisTemplate.execute(couponSecKill, Collections.emptyList(),
                couponId.toString(),
                customerId.toString(),
                String.valueOf(TimeUtils.toUnixTimestamp(LocalDateTime.now())));

        LuaResult res = new LuaResult();
        if (raw != null && raw.size() >= 2) {
            res.setCode(((Number) raw.get(0)).intValue());
            res.setMsg((String) raw.get(1));
        } else {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        Integer code = res.getCode();
        // 1. 领取成功
        if (1 == code) {
            // 异步更新数据库
            asyncUpdateDb(customerId, couponId);
        }
        return res;
    }

    /**
     * 发送消息~
     *
     * @param customerId
     * @param couponId
     */


    @Async("sharedThreadPool")
    public void asyncUpdateDb(Long customerId, Long couponId) {

        CouponInfoDto couponInfoDto = new CouponInfoDto();
        couponInfoDto.setId(couponId);
        couponInfoDto.setCustomerId(customerId);
        rabbitService.sendMessage(MqConst.EXCHANGE_COUPON, MqConst.ROUTING_COUPON_SUCCESS,
                MessageBuilder
                        .withBody(JSON.toJSONString(couponInfoDto)
                                .getBytes())
                        .build()
        );
    }


    // @Override
    // @Transactional
    // public Boolean receive(Long customerId, Long couponId) {
    //     // 1.查询 优惠券
    //     CouponInfo couponInfo = getById(couponId);
    //     if (couponId == null) {
    //         throw new GuiguException(ResultCodeEnum.DATA_ERROR);
    //     }
    //     // 2.优惠券过期日期判断
    //     if (couponInfo.getExpireTime().isBefore(LocalDateTime.now())) {
    //         throw new GuiguException(ResultCodeEnum.COUPON_EXPIRE);
    //     }
    //     // 3、校验库存，优惠券领取数量判断
    //     if (couponInfo.getPublishCount() != 0 && couponInfo.getReceiveCount() >= couponInfo.getPublishCount()) {
    //         throw new GuiguException(ResultCodeEnum.COUPON_LESS);
    //     }
    //
    //     RLock lock = null;
    //     try {
    //         // 初始化分布式锁
    //         // 每人领取限制  与 优惠券发行总数 必须保证原子性，使用couponId减少锁的粒度，增加并发能力
    //         lock = redissonClient.getLock(RedisConstant.COUPON_LOCK + couponId);
    //         boolean flag = lock.tryLock(RedisConstant.COUPON_LOCK_WAIT_TIME, RedisConstant.COUPON_LOCK_LEASE_TIME,
    //                 TimeUnit.SECONDS);
    //         if (flag) {
    //             // 4、校验每人限领数量
    //             if (couponInfo.getPerLimit() > 0) {
    //                 // 4.1、统计当前用户对当前优惠券的已经领取的数量
    //                 long count =
    //                         customerCouponMapper.selectCount(new LambdaQueryWrapper<CustomerCoupon>().eq
    //                         (CustomerCoupon::getCouponId, couponId).eq(CustomerCoupon::getCustomerId, customerId));
    //                 // 4.2、校验限领数量
    //                 if (count >= couponInfo.getPerLimit()) {
    //                     throw new GuiguException(ResultCodeEnum.COUPON_USER_LIMIT);
    //                 }
    //             }
    //
    //             // 5、更新优惠券领取数量
    //             int row = 0;
    //             if (couponInfo.getPublishCount() == 0) {// 没有限制
    //                 row = baseMapper.updateReceiveCount(couponId);
    //             } else {
    //                 row = baseMapper.updateReceiveCountByLimit(couponId);
    //             }
    //             if (row == 1) {
    //                 // 6、保存领取记录
    //                 this.saveCustomerCoupon(customerId, couponId, couponInfo.getExpireTime());
    //                 return Boolean.TRUE;
    //             }
    //         }
    //     } catch (Exception e) {
    //         e.printStackTrace();
    //     } finally {
    //         if (lock != null && lock.isLocked() && lock.isHeldByCurrentThread()) {
    //             lock.unlock();
    //         }
    //     }
    //     throw new GuiguException(ResultCodeEnum.COUPON_LESS);
    // }

    @Transactional
    @Override
    public Boolean publish(Long couponId) {

        CouponInfo couponInfo = getById(couponId);
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        if (CouponStatus.PUBLISHED.getStatus().equals(couponInfo.getStatus())) {
            throw new GuiguException(ResultCodeEnum.REPEAT_SUBMIT);
        }

        LambdaUpdateWrapper<CouponInfo> updateCouponInfo = new LambdaUpdateWrapper<CouponInfo>()
                .set(CouponInfo::getStatus, CouponStatus.PUBLISHED.getStatus())
                .eq(BaseEntity::getId, couponId);
        couponInfo.setStatus(CouponStatus.PUBLISHED.getStatus());
        boolean isSuccess = update(updateCouponInfo);
        if (!isSuccess) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        // 预热到缓存里~
        CouponInfoVo couponInfoVo = couponInfoConvert.toCouponInfoVo(couponInfo);
        Boolean aBoolean = couponPreheatService.preHeat(couponInfoVo);
        if (Boolean.FALSE.equals(aBoolean)) {
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        return Boolean.TRUE;
    }

    @Override
    public List<AvailableCouponVo> findAvailableCoupon(Long customerId, BigDecimal orderAmount) {
        List<NoUseCouponVo> noUseList = baseMapper.findNoUseList(customerId);
        if (CollectionUtils.isEmpty(noUseList)) {
            return Collections.emptyList();
        }
        return noUseList.stream().filter(noUseCouponVo -> {
                    // 1.过滤不符合的优惠券
                    BigDecimal amount = noUseCouponVo.getAmount();
                    // 1.1 校验现金券的合法性
                    if (Objects.equals(noUseCouponVo.getCouponType(), CouponType.CASH_COUPON.getType())) {
                        if (amount.compareTo(orderAmount) >= 0) {
                            return false;
                        }
                    }
                    BigDecimal conditionAmount = noUseCouponVo.getConditionAmount();
                    // 1.2 校验门槛
                    return conditionAmount.compareTo(orderAmount) <= 0;
                }).map(noUseCouponVo -> {
                    // 2. 映射
                    AvailableCouponVo availableCouponVo = couponInfoConvert.toAvailableCouponVo(noUseCouponVo);
                    availableCouponVo.setReduceAmount(discount(availableCouponVo, orderAmount));
                    return availableCouponVo;
                }).sorted(Comparator.comparing(
                        AvailableCouponVo::getReduceAmount,
                        Comparator.reverseOrder()  // 降序
                ))
                .toList();
    }

    @Override
    @Transactional
    public BigDecimal useCoupon(UseCouponForm useCouponForm) {
        Long customerCouponId = useCouponForm.getCustomerCouponId();
        // 获取乘客优惠券
        CustomerCoupon customerCoupon = customerCouponMapper.
                selectById(customerCouponId);
        if (customerCoupon == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 获取优惠券信息
        CouponInfo couponInfo = baseMapper.selectById(customerCoupon.getCouponId());
        if (couponInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        // 判断该优惠券是否为乘客所有
        if (!Objects.equals(customerCoupon.getCustomerId(), useCouponForm.getCustomerId())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //  校验现金券的合法性
        Integer couponType = couponInfo.getCouponType();
        BigDecimal conditionAmount = couponInfo.getConditionAmount();
        BigDecimal orderAmount = useCouponForm.getOrderAmount();
        if (Objects.equals(couponType, CouponType.CASH_COUPON.getType())) {
            BigDecimal amount = couponInfo.getAmount();
            if (amount.compareTo(orderAmount) >= 0) {
                throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
            }
        }
        // 校验门槛的合法性
        if (conditionAmount.compareTo(orderAmount) > 0) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        // 折扣了多少钱
        BigDecimal discountedPrice = getDiscountAmount(couponInfo, orderAmount);

        if (discountedPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);

        }

        int row = baseMapper.updateUseCount(couponInfo.getId());

        if (row != 1) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        CustomerCoupon updateCustomerCoupon = new CustomerCoupon();
        updateCustomerCoupon.setId(customerCoupon.getId());
        updateCustomerCoupon.setUsedTime(LocalDateTime.now());
        updateCustomerCoupon.setOrderId(useCouponForm.getOrderId());
        row = customerCouponMapper.updateById(updateCustomerCoupon);
        if (row != 1) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

        return discountedPrice;
    }

    @Transactional
    @Override
    public void handleCouponInfoDto(CouponInfoDto couponInfoDto) {
        Long couponId = couponInfoDto.getId();
        LambdaQueryWrapper<CouponInfo> eq = new LambdaQueryWrapper<CouponInfo>()
                .select(BaseEntity::getId, CouponInfo::getExpireTime)
                .eq(BaseEntity::getId, couponId);
        CouponInfo couponInfo = baseMapper.selectOne(eq);
        if (couponInfo == null) {
            log.error("此优惠券不存在");
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        couponInfoDto.setReceiveTime(LocalDateTime.now());
        couponInfoDto.setStatus(CustomerCouponStatus.unused.getStatus());
        // 1.发给用户
        Long customerId = couponInfoDto.getCustomerId();
        CustomerCoupon customerCoupon = new CustomerCoupon();
        customerCoupon.setCouponId(couponId);
        customerCoupon.setCustomerId(customerId);
        customerCoupon.setReceiveTime(couponInfoDto.getReceiveTime());
        customerCoupon.setStatus(CustomerCouponStatus.unused.getStatus());
        customerCoupon.setExpireTime(couponInfo.getExpireTime());
        int insert = customerCouponMapper.insert(customerCoupon);

        if (insert != 1) {
            log.error("优惠券=>{}添加到用户=>{}失败", couponId, customerId);
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
        // 增加优惠券的领取数量
        int update = baseMapper.incrReceiveCount(couponId);
        if (update != 1) {
            log.error("优惠券=>{}添加到用户=>{}失败", couponId, customerId);
            throw new GuiguException(ResultCodeEnum.UPDATE_ERROR);
        }
    }


    private static BigDecimal getDiscountAmount(CouponInfo couponInfo, BigDecimal orderAmount) {
        Integer couponType = couponInfo.getCouponType();
        if (Objects.equals(couponType, CouponType.CASH_COUPON.getType())) {
            return couponInfo.getAmount();
        } else {
            BigDecimal discount = couponInfo.getDiscount();
            BigDecimal discountedPrice = orderAmount.multiply(discount.multiply(new BigDecimal("0.1"))).setScale(2,
                    RoundingMode.HALF_UP);
            // 计算折扣金额（原价-折扣后价格）
            BigDecimal discountAmount = orderAmount.subtract(discountedPrice)
                    .setScale(2, RoundingMode.HALF_UP);
            return discountAmount;

        }

    }

    private static BigDecimal discount(AvailableCouponVo availableCouponVo, BigDecimal orderAmount) {
        Integer couponType = availableCouponVo.getCouponType();
        if (Objects.equals(couponType, CouponType.CASH_COUPON.getType())) {
            return availableCouponVo.getAmount();
        } else {
            BigDecimal discount = availableCouponVo.getDiscount();
            BigDecimal discountedPrice = orderAmount.multiply(discount.multiply(new BigDecimal("0.1"))).setScale(2,
                    RoundingMode.HALF_UP);
            // 计算折扣金额（原价-折扣后价格）
            BigDecimal discountAmount = orderAmount.subtract(discountedPrice)
                    .setScale(2, RoundingMode.HALF_UP);
            return discountAmount;

        }

    }


    public static void main(String[] args) {
        // BigDecimal bigDecimal = new BigDecimal("0.00");
        // System.out.println(bigDecimal.compareTo(BigDecimal.ZERO));
    }


    // private void saveCustomerCoupon(Long customerId, Long couponId, LocalDateTime expireTime) {
    //     CustomerCoupon customerCoupon = new CustomerCoupon();
    //     customerCoupon.setCustomerId(customerId);
    //     customerCoupon.setCouponId(couponId);
    //     customerCoupon.setStatus(1);
    //     customerCoupon.setReceiveTime(new Date());
    //     customerCoupon.setExpireTime(expireTime);
    //     customerCouponMapper.insert(customerCoupon);
    // }
}
