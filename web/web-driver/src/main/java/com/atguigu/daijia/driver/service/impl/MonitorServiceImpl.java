package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;


    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        Long driverId = AuthContextHolder.getUserId();
        // 1.是不是司机的订单
        return null;
    }
}
