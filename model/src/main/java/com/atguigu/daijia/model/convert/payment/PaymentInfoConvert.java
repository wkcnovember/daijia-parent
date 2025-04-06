package com.atguigu.daijia.model.convert.payment;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.payment.PaymentInfo;
import com.atguigu.daijia.model.form.payment.PaymentInfoForm;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/4/5 21:15
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface PaymentInfoConvert {

    PaymentInfo toPaymentInfo(PaymentInfoForm paymentInfoForm);
}
