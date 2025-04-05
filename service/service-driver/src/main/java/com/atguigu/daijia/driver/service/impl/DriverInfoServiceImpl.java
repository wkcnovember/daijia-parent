package com.atguigu.daijia.driver.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import com.atguigu.daijia.common.config.tencent.TencentCloudProperties;
import com.atguigu.daijia.common.constant.DriverConstant;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.mapper.*;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.driver.service.DriverInfoService;
import com.atguigu.daijia.model.constants.login.LoginCannelConstants;
import com.atguigu.daijia.model.convert.driver.DriverInfoConvert;
import com.atguigu.daijia.model.convert.driver.DriverSetConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.driver.*;
import com.atguigu.daijia.model.form.driver.DriverFaceModelForm;
import com.atguigu.daijia.model.form.driver.UpdateDriverAuthInfoForm;
import com.atguigu.daijia.model.vo.driver.DriverAuthInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverInfoVo;
import com.atguigu.daijia.model.vo.driver.DriverLoginVo;
import com.atguigu.daijia.model.vo.driver.DriverSetVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.iai.v20200303.IaiClient;
import com.tencentcloudapi.iai.v20200303.models.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
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

    @Resource
    private TencentCloudProperties tencentCloudProperties;

    @Resource
    private DriverSetConvert driverSetConvert;
    @Resource
    private DriverFaceRecognitionMapper driverFaceRecognitionMapper;


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
        if (null == driverInfo) {

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
            driverSet.setAutoAccept(DriverConstant.NOT_AUTO_ORDER);// 0：否 1：是
            driverSetMapper.insert(driverSet);

            // 初始化司机账户信息
            DriverAccount driverAccount = new DriverAccount();
            driverAccount.setDriverId(driverInfo.getId());
            driverAccountMapper.insert(driverAccount);
        }
        // 记录司机登录信息
        DriverLoginLog driverLoginLog = new DriverLoginLog();
        driverLoginLog.setDriverId(driverInfo.getId());
        driverLoginLog.setMsg(LoginCannelConstants.WECHAT_MP_CHANNEL);
        driverLoginLogMapper.insert(driverLoginLog);
        return driverInfo.getId();
    }

    @Override
    public DriverLoginVo getDriverInfo(Long driverId) {
        // 根据司机id获取司机信息
        DriverInfo driverInfo = baseMapper.selectById(driverId);
        if (null == driverInfo) throw new GuiguException(ResultCodeEnum.DATA_ERROR);

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
            if (null == driverInfo) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            return driverInfoConvert.toDriverAuthInfoVo(driverInfo);
        }, sharedThreadPool);
        try {
            // 2. 获取基本信息并设置超时
            DriverAuthInfoVo driverAuthInfoVo = infoFuture.get(2, TimeUnit.SECONDS);

            // 3. 并行处理所有图片URL转换
            List<CompletableFuture<Void>> imageFutures = new ArrayList<>();

            // 处理身份证相关图片
            imageFutures.add(processImageAsync(driverAuthInfoVo::setIdcardBackShowUrl,
                    driverAuthInfoVo.getIdcardBackUrl()));
            imageFutures.add(processImageAsync(driverAuthInfoVo::setIdcardFrontShowUrl,
                    driverAuthInfoVo.getIdcardFrontUrl()));
            imageFutures.add(processImageAsync(driverAuthInfoVo::setIdcardHandShowUrl,
                    driverAuthInfoVo.getIdcardHandUrl()));

            // 处理驾驶证相关图片
            imageFutures.add(processImageAsync(driverAuthInfoVo::setDriverLicenseBackShowUrl,
                    driverAuthInfoVo.getDriverLicenseBackUrl()));
            imageFutures.add(processImageAsync(driverAuthInfoVo::setDriverLicenseFrontShowUrl,
                    driverAuthInfoVo.getDriverLicenseFrontUrl()));
            imageFutures.add(processImageAsync(driverAuthInfoVo::setDriverLicenseHandShowUrl,
                    driverAuthInfoVo.getDriverLicenseHandUrl()));

            // 4. 等待所有图片处理完成
            CompletableFuture.allOf(imageFutures.toArray(new CompletableFuture[0]))
                    .get(8, TimeUnit.SECONDS); // 总超时时间10秒(2+8)

            return driverAuthInfoVo;
        } catch (TimeoutException e) {
            log.warn("获取司机的认证信息超时, 原因={}", e.getMessage());
            throw new GuiguException(ResultCodeEnum.TIMEOUT_ERROR);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("获取司机[{}]认证信息被中断", driverId);
            throw new GuiguException(ResultCodeEnum.SYSTEM_ERROR);
        }catch (Exception e) {
            if (e.getCause() instanceof GuiguException) {
                throw (GuiguException) e.getCause();
            }
            log.warn("获取司机的认证信息失败, 原因={}", e.getMessage());
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }


    }

    // 辅助方法：异步处理图片URL
    private CompletableFuture<Void> processImageAsync(Consumer<String> setter, String imageUrl) {
        return CompletableFuture.runAsync(() -> {
            if (StringUtils.isNotBlank(imageUrl)) {
                setter.accept(cosService.getImageUrl(imageUrl));
            }
        }, sharedThreadPool);
    }

    // 创建司机人脸模型
    @Override
    public Boolean creatDriverFaceModel(DriverFaceModelForm driverFaceModelForm) {
        // 根据司机id获取司机信息
        LambdaQueryWrapper<DriverInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BaseEntity::getId, driverFaceModelForm.getDriverId())
                .select(BaseEntity::getId, DriverInfo::getGender, DriverInfo::getName);
        DriverInfo driverInfo =
                baseMapper.selectOne(queryWrapper);
        try {

            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            IaiClient client = getIaiClient();
            // 实例化一个请求对象,每个接口都会对应一个request对象
            CreatePersonRequest req = new CreatePersonRequest();
            // 设置相关值
            req.setGroupId(tencentCloudProperties.getPersonGroupId());
            // 基本信息
            req.setPersonId(String.valueOf(driverInfo.getId()));
            req.setGender(Long.parseLong(driverInfo.getGender()));
            req.setQualityControl(4L);
            req.setUniquePersonControl(4L);
            req.setPersonName(driverInfo.getName());
            req.setImage(driverFaceModelForm.getImageBase64());


            // 返回的resp是一个CreatePersonResponse的实例，与请求对象对应
            CreatePersonResponse resp = client.CreatePerson(req);

            String faceId = resp.getFaceId();
            if (StringUtils.isNotBlank(faceId)) {
                driverInfo.setName(null);
                driverInfo.setGender(null);
                driverInfo.setAuthStatus(DriverConstant.AuthStatus.IN_REVIEW.getCode());
                driverInfo.setFaceModelId(faceId);
                baseMapper.updateById(driverInfo);
            }
        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public DriverSetVo getDriverSet(Long driverId) {
        LambdaQueryWrapper<DriverSet> eq = new LambdaQueryWrapper<DriverSet>()
                .select(DriverSet::getDriverId, DriverSet::getServiceStatus,
                        DriverSet::getAcceptDistance, DriverSet::getOrderDistance,
                        DriverSet::getAutoAccept
                )
                .eq(DriverSet::getDriverId, driverId);
        DriverSet driverSet = driverSetMapper.selectOne(eq);
        if (null == driverSet) throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        return driverSetConvert.toDriverSetVo(driverSet);
    }

    // 判断司机当日是否进行过人脸识别
    @Override
    public Boolean isFaceRecognition(Long driverId) {
        // 根据司机id + 当日日期进行查询
        LambdaQueryWrapper<DriverFaceRecognition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverFaceRecognition::getDriverId, driverId);
        // 年-月-日 格式
        wrapper.eq(DriverFaceRecognition::getFaceDate, new DateTime().toString("yyyy-MM-dd"));
        // 调用mapper方法
        Long count = driverFaceRecognitionMapper.selectCount(wrapper);

        return count != 0;
    }

    // 人脸识别
    @Override
    public Boolean verifyDriverFace(DriverFaceModelForm driverFaceModelForm) {
        // 1 照片比对
        Long driverId = driverFaceModelForm.getDriverId();
        try {
            // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
            // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
            // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
            IaiClient client = getIaiClient();
            // 实例化一个请求对象,每个接口都会对应一个request对象
            VerifyFaceRequest req = new VerifyFaceRequest();
            // 设置相关参数
            req.setImage(driverFaceModelForm.getImageBase64());
            req.setPersonId(String.valueOf(driverFaceModelForm.getDriverId()));

            // 返回的resp是一个VerifyFaceResponse的实例，与请求对象对应
            VerifyFaceResponse resp = client.VerifyFace(req);
            // 输出json格式的字符串回包
            // System.out.println(AbstractModel.toJsonString(resp));

            // 静态对比失败~
            if (Boolean.FALSE.equals(resp.getIsMatch())) {
                throw new GuiguException(ResultCodeEnum.DATA_ERROR);
            }


            // todo 动态对比.需要环境支持 暂时使用静态对比
            DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
            driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
            driverFaceRecognition.setFaceDate(new Date());
            driverFaceRecognitionMapper.insert(driverFaceRecognition);
            return Boolean.TRUE;
            // 照片比对成功
            // 2 如果照片比对成功，静态活体检测
            // Boolean isSuccess = this.
            //         detectLiveFace(driverId, driverFaceModelForm.getImageBase64());
            // // 3 如果静态活体都 检测通过，添加数据到认证表里面
            // if (Boolean.TRUE.equals(isSuccess)) {
            //     DriverFaceRecognition driverFaceRecognition = new DriverFaceRecognition();
            //     driverFaceRecognition.setDriverId(driverFaceModelForm.getDriverId());
            //     driverFaceRecognition.setFaceDate(new Date());
            //     driverFaceRecognitionMapper.insert(driverFaceRecognition);
            //     return Boolean.TRUE;
            // }
        } catch (TencentCloudSDKException e) {
            log.warn("用户={},人脸识别失败~,原因=>{}", driverId, e.getMessage());
            // System.out.println(e.toString());
            // return Boolean.FALSE;
        }
        return Boolean.FALSE;

    }

    @Override
    public Boolean updateServiceStatus(Long driverId, Integer status) {
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DriverSet::getDriverId, driverId);
        DriverSet driverSet = new DriverSet();
        driverSet.setServiceStatus(status);
        driverSetMapper.update(driverSet, wrapper);
        return Boolean.TRUE;
    }

    @Override
    public Map<Long, DriverSetVo> getDriverSetMap(List<Long> driverIds) {
        if (CollectionUtils.isEmpty(driverIds)) {
            return null;
        }
        LambdaQueryWrapper<DriverSet> wrapper = new LambdaQueryWrapper<DriverSet>()
                .select(DriverSet::getDriverId,
                        DriverSet::getServiceStatus,
                        DriverSet::getOrderDistance,
                        DriverSet::getAcceptDistance,
                        DriverSet::getAutoAccept
                )
                // 正在的司机
                .eq(DriverSet::getServiceStatus, DriverConstant.ServiceStatus.ACCEPTING_ORDERS.getStatus())
                .in(DriverSet::getDriverId, driverIds);
        List<DriverSet> driverSets = driverSetMapper.selectList(wrapper);
        Map<Long, DriverSetVo> map = driverSets.stream()
                .map(driverSet -> driverSetConvert.toDriverSetVo(driverSet))
                .collect(Collectors.toMap(DriverSetVo::getDriverId, driverSetVo -> driverSetVo));
        return map;
    }

    @Override
    public DriverInfoVo getDriverInfoVo(Long driverId) {
        LambdaQueryWrapper<DriverInfo> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(DriverInfo::getName, DriverInfo::getPhone,
                        DriverInfo::getGender, DriverInfo::getAvatarUrl,
                        DriverInfo::getDriverLicenseIssueDate, DriverInfo::getScore,
                        DriverInfo::getOrderCount)
                .eq(BaseEntity::getId, driverId);
        DriverInfo driverInfo = baseMapper.selectOne(wrapper);
        if (driverInfo == null) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        DriverInfoVo driverInfoVo = driverInfoConvert.toDriverInfoVo(driverInfo);
        // 驾龄
        Integer driverLicenseAge =
                new DateTime().getYear() - new DateTime(driverInfo.getDriverLicenseIssueDate()).getYear() + 1;
        driverInfoVo.setDriverLicenseAge(driverLicenseAge);
        return driverInfoVo;
    }

    @Override
    public String getDriverOpenId(Long driverId) {
        DriverInfo driverInfo =
                baseMapper.selectOne(new LambdaQueryWrapper<DriverInfo>()
                        .select(DriverInfo::getWxOpenId).eq(BaseEntity::getId, driverId));
        return driverInfo == null ? null : driverInfo.getWxOpenId();
    }

    // 人脸静态活体检测
    private Boolean detectLiveFace(Long driverId, String imageBase64) {
        try {
            IaiClient client = getIaiClient();
            // 实例化一个请求对象,每个接口都会对应一个request对象
            DetectLiveFaceRequest req = new DetectLiveFaceRequest();
            req.setImage(imageBase64);
            // 返回的resp是一个DetectLiveFaceResponse的实例，与请求对象对应
            DetectLiveFaceResponse resp = client.DetectLiveFace(req);
            // 输出json格式的字符串回包
            // System.out.println(DetectLiveFaceResponse.toJsonString(resp));
            return resp.getIsLiveness();
            // if (resp.getIsLiveness()) {
            //     return true;
            // }
        } catch (TencentCloudSDKException e) {
            log.warn("用户={},人脸活体识别失败~,原因=>{}", driverId, e.getMessage());
        }
        return Boolean.FALSE;
    }

    private IaiClient getIaiClient() {
        // 实例化一个认证对象，入参需要传入腾讯云账户 SecretId 和 SecretKey，此处还需注意密钥对的保密
        // 代码泄露可能会导致 SecretId 和 SecretKey 泄露，并威胁账号下所有资源的安全性。以下代码示例仅供参考，建议采用更安全的方式来使用密钥，请参见：https://cloud.tencent.com/document/product/1278/85305
        // 密钥可前往官网控制台 https://console.cloud.tencent.com/cam/capi 进行获取
        Credential cred = new Credential(tencentCloudProperties.getSecretId(),
                tencentCloudProperties.getSecretKey());
        // 实例化一个http选项，可选的，没有特殊需求可以跳过
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("iai.tencentcloudapi.com");
        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        // 实例化要请求产品的client对象,clientProfile是可选的
        IaiClient client = new IaiClient(cred, tencentCloudProperties.getRegion(),
                clientProfile);
        return client;
    }
}
