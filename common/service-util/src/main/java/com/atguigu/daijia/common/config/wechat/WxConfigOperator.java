package com.atguigu.daijia.common.config.wechat;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.api.impl.WxMaServiceImpl;
import cn.binarywang.wx.miniapp.config.impl.WxMaDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Author 柯佳元
 * @Create 2025/3/17 9:00
 * @Version 1.0
 * Description: 微信接口操作
 */

@Configuration
public class WxConfigOperator {

    @Bean
    public WxMaService wxMaService(@Autowired WxConfigProperties wxConfigProperties) {
        // 微信小程序id和秘钥
        WxMaDefaultConfigImpl wxMaConfig = new WxMaDefaultConfigImpl();
        wxMaConfig.setAppid(wxConfigProperties.getAppId());
        wxMaConfig.setSecret(wxConfigProperties.getSecret());

        WxMaService service = new WxMaServiceImpl();
        service.setWxMaConfig(wxMaConfig);
        return service;
    }


}
