package com.atguigu.daijia.model.convert.map;

import com.atguigu.daijia.model.convert.GlobalMapperConfig;
import com.atguigu.daijia.model.form.customer.ExpectOrderForm;
import com.atguigu.daijia.model.form.customer.SubmitOrderForm;
import com.atguigu.daijia.model.form.map.CalculateDrivingLineForm;
import org.mapstruct.Mapper;

/**
 * @Author 柯佳元
 * @Create 2025/3/21 10:53
 * @Version 1.0
 * Description:
 */
@Mapper(config = GlobalMapperConfig.class)
public interface CalculateDrivingLineConvert {

    CalculateDrivingLineForm toCalculateDrivingLine(ExpectOrderForm expectOrderForm);

    CalculateDrivingLineForm toCalculateDrivingLineBySb(SubmitOrderForm submitOrderForm);
}
