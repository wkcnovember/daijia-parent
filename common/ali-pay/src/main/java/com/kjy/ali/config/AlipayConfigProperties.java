package com.kjy.ali.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "alipay")
@Data
public class AlipayConfigProperties {

    private  String appId;

    // 商户私钥
    private  String appPrivateKey;
    // 阿里公钥
    private  String alipayPublicKey;

    // 通知回调地址
    private   String notifyUrl = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/return_uxrl.jsp";
    // 页面跳转同步通知页面路径 需http://或者https://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问 商户可以自定义同步跳转地址
     public  String return_url = "http://商户网关地址/alipay.trade.wap.pay-JAVA-UTF-8/return_url.jsp"; // 很少用

    // 请求网关地址最新  https://openapi-sandbox.dl.alipaydev.com/gateway.do
    private  String url = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    // 编码
    private  String charset ;
    // 返回格式
    private  String format = "JSON";
    // 支付宝公钥
//	public static String ALIPAY_PUBLIC_KEY = "";
    // 日志记录目录
    private  String logPath = "/log";
    // RSA2
    private  String signType = "RSA2";
}
