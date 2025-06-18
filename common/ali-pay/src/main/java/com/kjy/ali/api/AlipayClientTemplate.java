package com.kjy.ali.api;

import com.alibaba.fastjson.JSON;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.kjy.ali.config.AlipayConfigProperties;
import com.kjy.ali.vo.PayVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;


@Slf4j
public class AlipayClientTemplate {

    @Resource
    private AlipayConfigProperties alipayConfigProperties;


    public AlipayClient alipayClient() {
        return new DefaultAlipayClient(
                alipayConfigProperties.getUrl(),
                alipayConfigProperties.getAppId(),
                alipayConfigProperties.getAppPrivateKey(),
                alipayConfigProperties.getFormat(),
                alipayConfigProperties.getCharset(),
                alipayConfigProperties.getAlipayPublicKey(),
                alipayConfigProperties.getSignType());
    }

    public String pay(PayVo vo) throws AlipayApiException {
        AlipayClient alipayClient = alipayClient();
        // 2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(alipayConfigProperties.getReturn_url());
        alipayRequest.setNotifyUrl(alipayConfigProperties.getNotifyUrl());

        // 商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        // 付款金额，必填
        String total_amount = vo.getTotal_amount();
        // 订单名称，必填
        String subject = vo.getSubject();
        // 商品描述，可空
        String body = vo.getBody();

        // 构造sdk的客户端对象
        // AlipayTradePrecreateModel model = new AlipayTradePrecreateModel();
        // model.setOutTradeNo(out_trade_no);
        // model.setTotalAmount(total_amount);
        // model.setSubject(subject);
        // model.setProductCode("FAST_INSTANT_TRADE_PAY");
        // model.setTimeExpire("2m");
        // model.setBody(body);
        // alipayRequest.setBizModel(model);


        Map<String, Object> bizContent = new HashMap<>();
        bizContent.put("out_trade_no", out_trade_no);
        bizContent.put("total_amount", total_amount);
        bizContent.put("subject", StringUtils.left(subject, 256));
        bizContent.put("body", body);
        bizContent.put("timeout_express", "2m");
        bizContent.put("product_code", "FAST_INSTANT_TRADE_PAY");

        alipayRequest.setBizContent(JSON.toJSONString(bizContent));

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        // 会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        log.info("支付宝的响应：\n" + result);
        return result;

    }

}
