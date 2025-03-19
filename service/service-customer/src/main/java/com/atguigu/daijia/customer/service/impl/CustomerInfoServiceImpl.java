package com.atguigu.daijia.customer.service.impl;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.customer.mapper.CustomerInfoMapper;
import com.atguigu.daijia.customer.mapper.CustomerLoginLogMapper;
import com.atguigu.daijia.customer.service.CustomerInfoService;
import com.atguigu.daijia.model.convert.customer.CustomerInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.customer.CustomerInfo;
import com.atguigu.daijia.model.entity.customer.CustomerLoginLog;
import com.atguigu.daijia.model.form.customer.UpdateWxPhoneForm;
import com.atguigu.daijia.model.vo.customer.CustomerLoginVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.error.WxErrorException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CustomerInfoServiceImpl extends ServiceImpl<CustomerInfoMapper, CustomerInfo> implements CustomerInfoService {

    @Resource
    private WxMaService wxMaService;
    @Resource
    private CustomerLoginLogMapper customerLoginLogMapper;

    @Resource
    private CustomerInfoConvert customerInfoConvert;


    @Override
    public Long login(String code) {
        String openid;
        try {
            // 1.通过code去微信接口获取openId
            WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
            openid = sessionInfo.getOpenid();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // 2.判断是否第一次登录 此 openid 对应的用户,如果是添加到表里~
        CustomerInfo customerInfo =
                baseMapper.selectOne(new LambdaQueryWrapper<CustomerInfo>().eq(CustomerInfo::getWxOpenId, openid));

        if (null == customerInfo) {
            customerInfo = new CustomerInfo();
            customerInfo.setNickname(String.valueOf(System.currentTimeMillis()));
            customerInfo.setAvatarUrl("https://oss.aliyuncs.com/aliyun_id_photo_bucket/default_handsome.jpg");
            customerInfo.setWxOpenId(openid);
            baseMapper.insert(customerInfo);
        }

        // 4 记录登录日志信息
        CustomerLoginLog customerLoginLog = new CustomerLoginLog();
        customerLoginLog.setCustomerId(customerInfo.getId());
        customerLoginLog.setMsg("小程序登录");
        customerLoginLogMapper.insert(customerLoginLog);

        // 5 返回用户id
        return customerInfo.getId();
    }

    @Override
    public CustomerLoginVo getCustomerInfo(Long customerId) {
        CustomerInfo customerInfo = getById(customerId);
        if (null == customerInfo) return null;
        CustomerLoginVo customerLoginVo = customerInfoConvert.toCustomerLoginVo(customerInfo);
        String phone = customerInfo.getPhone();
        boolean isBindPhone = StringUtils.isNotBlank(phone);
        customerLoginVo.setIsBindPhone(isBindPhone);
        return customerLoginVo;
    }

    @Override
    public Boolean updateWxPhoneNumber(UpdateWxPhoneForm updateWxPhoneForm) {

        // 查询有没有
        // Long customerId = updateWxPhoneForm.getCustomerId();
        // CustomerInfo customerInfo = baseMapper.selectOne(
        //         new LambdaQueryWrapper<CustomerInfo>().eq(BaseEntity::getId, customerId)
        //                 .select(BaseEntity::getId)
        // );
        // if (Objects.isNull(customerInfo)) {
        //     return Boolean.FALSE;
        // }

        try {
            WxMaPhoneNumberInfo phoneNoInfo = wxMaService.getUserService().getPhoneNoInfo(updateWxPhoneForm.getCode());
            String phoneNumber = phoneNoInfo.getPhoneNumber();
            CustomerInfo customerInfo = new CustomerInfo();
            customerInfo.setId(updateWxPhoneForm.getCustomerId());
            customerInfo.setPhone(phoneNumber);
            baseMapper.updateById(customerInfo);
            return Boolean.TRUE;
        } catch (WxErrorException e) {
            log.error("更新用户手机号码失败,原因={}", e.getMessage());
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }

    }
}
