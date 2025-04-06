package com.atguigu.daijia.order.controller;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.extra.qrcode.QrConfig;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.kjy.ali.api.AlipayClientTemplate;
import com.kjy.ali.vo.PayVo;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.OutputStream;

/**
 * @Author 柯佳元
 * @Create 2025/4/5 17:14
 * @Version 1.0
 * Description:
 */

@RestController
public class TestController {

    @Resource
    private AlipayClientTemplate alipayClientTemplate;

    /**
     * 生成支付宝支付二维码图片流
     * @param response HttpServletResponse
     */
    @GetMapping(value = "/qrcode/alipay", produces = MediaType.IMAGE_PNG_VALUE)
    public void generateAlipayQrCode(
            HttpServletResponse response) throws Exception {

        // 设置二维码配置
        QrConfig config = new QrConfig(300, 300);
        config.setMargin(1); // 设置边距
        config.setErrorCorrection(ErrorCorrectionLevel.M); // 纠错级别

        // 直接输出到响应流，避免内存占用过高
        response.setContentType("image/png");
        try (OutputStream out = response.getOutputStream()) {
            QrCodeUtil.generate("http://localhost:8505/aliPayOrder?orderSn=1", config, "png", out);
        }
    }
    @GetMapping(value = "aliPayOrder", produces = "text/html")
    @Operation(summary = "订单支付")
    public String aliPayOrder(@RequestParam("orderSn") String orderSn) {
        try {
            PayVo payVo = new PayVo();
            payVo.setSubject("iphpne16");
            payVo.setTotal_amount("5999.99");
            payVo.setOut_trade_no("213123");
            payVo.setBody("好玩");
            String pay = alipayClientTemplate.pay(payVo);
            return pay;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
