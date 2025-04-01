package com.atguigu.daijia.model.convert.order;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.entity.order.OrderBill;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderProfitsharing;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.model.form.order.UpdateOrderBillForm;
import com.atguigu.daijia.model.vo.order.OrderInfoVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

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

    @Mapping(target = "payAmount",source = "totalAmount")
    OrderBill toOrderBill(UpdateOrderBillForm updateOrderBillForm);

    @Mapping(target = "ruleId",source = "profitsharingRuleId")
    OrderProfitsharing toOrderProfitsharing(UpdateOrderBillForm updateOrderBillForm);

}
