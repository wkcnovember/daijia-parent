package com.atguigu.daijia.order.service.impl;

import com.atguigu.daijia.model.convert.order.OrderInfoConvert;
import com.atguigu.daijia.model.entity.base.BaseEntity;
import com.atguigu.daijia.model.entity.order.OrderInfo;
import com.atguigu.daijia.model.entity.order.OrderStatusLog;
import com.atguigu.daijia.model.enums.OrderStatus;
import com.atguigu.daijia.model.form.order.OrderInfoForm;
import com.atguigu.daijia.order.mapper.OrderInfoMapper;
import com.atguigu.daijia.order.mapper.OrderStatusLogMapper;
import com.atguigu.daijia.order.service.OrderInfoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {


    @Resource
    private OrderInfoConvert orderInfoConvert;
    @Resource
    private OrderStatusLogMapper orderStatusLogMapper;

    @Override
    @Transactional
    public Long saveOrderInfo(OrderInfoForm orderInfoForm) {
        OrderInfo orderInfo = orderInfoConvert.toOrderInfo(orderInfoForm);
        //订单号
        String orderNo = UUID.randomUUID().toString().replaceAll("-","");
        orderInfo.setOrderNo(orderNo);
        orderInfo.setStatus(OrderStatus.WAITING_ACCEPT.getStatus());
        save(orderInfo);
        log(orderInfo.getId(),orderInfo.getStatus());
        return orderInfo.getId();
    }

    @Override
    public Integer getOrderStatus(Long orderId) {
        OrderInfo orderInfo =
                baseMapper.selectOne(new LambdaQueryWrapper<OrderInfo>().eq(BaseEntity::getId, orderId).select(OrderInfo::getStatus));
        if(orderInfo == null) return OrderStatus.NULL_ORDER.getStatus();

        return orderInfo.getStatus();
    }

    void log(long orderId,Integer status) {
        OrderStatusLog orderStatusLog = new OrderStatusLog();
        orderStatusLog.setOrderId(orderId);
        orderStatusLog.setOrderStatus(status);
        orderStatusLog.setOperateTime(new Date());
        orderStatusLogMapper.insert(orderStatusLog);
    }
}
