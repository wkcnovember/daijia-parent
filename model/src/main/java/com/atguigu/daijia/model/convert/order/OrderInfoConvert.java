package com.atguigu.daijia.model.convert.order;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.vo.order.CurrentOrderInfoVo;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/3/21 13:01
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface OrderInfoConvert {

    OrderInfo toOrderInfo(OrderInfoForm orderInfoForm);

    OrderInfoForm toOrderInfoForm(SubmitOrderForm submitOrderForm);

    OrderInfoVo toOrderInfoVo(OrderInfo orderInfo);

}
