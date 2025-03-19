package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.mapper.DriverAccountMapper;
import com.atguigu.daijia.driver.mapper.DriverInfoMapper;
import com.atguigu.daijia.driver.mapper.DriverLoginLogMapper;
import com.atguigu.daijia.driver.mapper.DriverSetMapper;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.convert.driver.DriverInfoConvert;
import com.atguigu.daijia.model.entity.driver.DriverAccount;
import com.atguigu.daijia.model.entity.driver.DriverInfo;
import com.atguigu.daijia.model.entity.driver.DriverLoginLog;
import com.atguigu.daijia.model.entity.driver.DriverSet;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.*;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class DriverInfoServiceImpl extends ServiceImpl<DriverInfoMapper, DriverInfo> implements DriverInfoService {


    @Resource
    private WxMaService wxMaService;
    @Resource
    private DriverSetMapper driverSetMapper;
    @Resource
    private DriverAccountMapper driverAccountMapper;
    @Resource
    private DriverLoginLogMapper driverLoginLogMapper;

    @Resource
    private DriverInfoConvert driverInfoConvert;

    @Resource
    private CosService cosService;

    @Resource
    private ThreadPoolExecutor sharedThreadPool;


    /**
     * 小程序登录
     *
     * @param code
     * @return
     */
    @Override
    @Transactional
    public Long login(String code) {
        String openid;
        try {
            // 根据code + 小程序id + 秘钥请求微信接口，返回openid
            WxMaJscode2SessionResult info = wxMaService.getUserService().getSessionInfo(code);
            openid = info.getOpenid();

        } catch (WxErrorException e) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 根据openid查询是否第一次登录
        if (StringUtils.isBlank(openid)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        DriverInfo driverInfo = getOne(new LambdaQueryWrapper<DriverInfo>().eq(DriverInfo::getWxOpenId, openid));
        if (Objects.isNull(driverInfo)) {

            // 添加司机基本信息
            driverInfo = new DriverInfo();
            driverInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            driverInfo.setAvatarUrl(DriverConstant.DRIVER_DEFAULT_AVATAR);
            driverInfo.setWxOpenId(openid);
            baseMapper.insert(driverInfo);


            // 初始化司机设置
            DriverSet driverSet = new DriverSet();
            driverSet.setDriverId(driverInfo.getId());
            driverSet.setOrderDistance(new BigDecimal(DriverConstant.ORDER_DEFAULT_DISTANCE));// 0：无限制
            driverSet.setAcceptDistance(new BigDecimal(DriverConstant.ACCEPT_DISTANCE));// 默认接单范围：5公里
            driverSet.setIsAutoAccept(DriverConstant.NOT_AUTO_ORDER);// 0：否 1：是
            driverSetMapper.insert(driverSet);

            // 初始化司机账户信息
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }
        // 记录司机登录信息
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg("小程序登录");
        driverLoginLogMapper.insert(driverLoginLog);
        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        // 根据司机id获取司机信息
        DriverInfo driverInfo = baseMapper.selectById(driverId);
        if (Objects.isNull(driverInfo)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);

        // driverInfo -- DriverLoginVo
        DriverLoginVo driverLoginVo = driverInfoConvert.toDriverLoginVo(driverInfo);


        // 是否建档人脸识别
        String faceModelId = driverInfo.getFaceModelId();

        driverLoginVo.setIsArchiveFace(StringUtils.isNotBlank(faceModelId));

        return driverLoginVo;
    }

    @Override
    public Boolean updateDriverAuthInfo(UpdateDriverAuthInfoForm updateDriverAuthInfoForm) {
        DriverInfo driverInfo = driverInfoConvert.toDriverInfo(updateDriverAuthInfoForm);
        return this.updateById(driverInfo);
    }


    // public static void main(String[] args) {
    //     ExecutorService executorService = Executors.newFixedThreadPool(10);
    //     CompletableFuture<ArrayList<String>> t1 = CompletableFuture.supplyAsync(() -> {
    //         ArrayList<String> objects = new ArrayList<>();
    //         objects.add("首要");
    //         return objects;
    //     });
    //     CompletableFuture<Void> future = t1.thenAccept((x) -> {
    //         x.add("天才");
    //     });
    //
    //     try {
    //         CompletableFuture.allOf(t1,future).get();
    //     } catch (InterruptedException e) {
    //         throw new RuntimeException(e);
    //     } catch (ExecutionException e) {
    //         throw new RuntimeException(e);
    //     }
    //     System.out.println(t1.join());
    // }

    @Override
    public DriverAuthInfoVo getDriverAuthInfo(Long driverId) {


        CompletableFuture<DriverAuthInfoVo> infoFuture = CompletableFuture.supplyAsync(() -> {
            DriverInfo driverInfo = baseMapper.selectById(driverId);
            if (Objects.isNull(driverInfo)) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            return driverInfoConvert.toDriverAuthInfoVo(driverInfo);
        }, sharedThreadPool);

        CompletableFuture<Void> idCardBackUrlFuture = infoFuture.thenAccept((driverAuthInfoVo) -> {
            if (StringUtils.isNotBlank(driverAuthInfoVo.getIdcardBackUrl()))
                driverAuthInfoVo.setIdcardBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardBackUrl()));
        });
        CompletableFuture<Void> idCardFrontFuture = infoFuture.thenAccept((driverAuthInfoVo) -> {
            if (StringUtils.isNotBlank(driverAuthInfoVo.getIdcardFrontUrl()))
                driverAuthInfoVo.setIdcardFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardFrontUrl()));
        });

        CompletableFuture<Void> idcardHandFuture = infoFuture.thenAccept((driverAuthInfoVo) -> {
            if (StringUtils.isNotBlank(driverAuthInfoVo.getIdcardHandUrl()))
                driverAuthInfoVo.setIdcardHandShowUrl(cosService.getImageUrl(driverAuthInfoVo.getIdcardHandUrl()));
        });

        CompletableFuture<Void> driverLicenseFrontUrlFuture = infoFuture.thenAccept((driverAuthInfoVo) -> {
            if (StringUtils.isNotBlank(driverAuthInfoVo.getDriverLicenseFrontUrl()))
                driverAuthInfoVo.setDriverLicenseFrontShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseFrontUrl()));
        });

        CompletableFuture<Void> driverLicenseBackUrlFuture = infoFuture.thenAccept((driverAuthInfoVo) -> {
            if (StringUtils.isNotBlank(driverAuthInfoVo.getDriverLicenseBackUrl()))
                driverAuthInfoVo.setDriverLicenseBackShowUrl(cosService.getImageUrl(driverAuthInfoVo.getDriverLicenseBackUrl()));

        });

        try {
            CompletableFuture.allOf(infoFuture, idCardFrontFuture, idCardBackUrlFuture,
                    idcardHandFuture, driverLicenseBackUrlFuture, driverLicenseFrontUrlFuture).get(10,
                    TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("获取司机的认证信息超时, 原因={}", e.getMessage());
            throw new GuiguException(ResultCodeEnum.TIMEOUT_ERROR);
        } catch (Exception e) {
            log.error("获取司机的认证信息失败, 原因={}", e.getMessage());
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }


        return infoFuture.join();

    }
}
