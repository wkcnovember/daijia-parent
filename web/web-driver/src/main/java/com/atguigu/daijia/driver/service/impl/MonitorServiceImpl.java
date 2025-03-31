package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.common.util.AuthContextHolder;
import com.atguigu.daijia.driver.client.CiFeignClient;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.entity.order.OrderMonitor;
import com.atguigu.daijia.model.entity.order.OrderMonitorRecord;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import com.atguigu.daijia.model.vo.order.TextAuditingVo;
import com.atguigu.daijia.order.client.OrderInfoFeignClient;
import com.atguigu.daijia.order.client.OrderMonitorFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class MonitorServiceImpl implements MonitorService {

    @Resource
    private OrderInfoFeignClient orderInfoFeignClient;
    @Resource
    private FileService fileService;
    @Resource
    private OrderMonitorFeignClient orderMonitorFeignClient;

    @Resource
    private CiFeignClient ciFeignClient;



    @Override
    public Boolean upload(MultipartFile file, OrderMonitorForm orderMonitorForm) {
        Long orderId = orderMonitorForm.getOrderId();
        Long driverId = AuthContextHolder.getUserId();
        // 1.校验合法性
        Result<Boolean> driverOrderRes = orderInfoFeignClient.isDriverOrder(driverId, orderId);
        driverOrderRes.throwOnFailureOrDataIsNull();
        if(Boolean.FALSE.equals(driverOrderRes.getData())) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }

        //上传对话文件
        CosUploadVo upload = fileService.upload(file);
        String url = upload.getUrl();
        log.info("录音文件的路径={}", url);


        //保存订单监控记录数
        OrderMonitorRecord orderMonitorRecord = new OrderMonitorRecord();
        orderMonitorRecord.setOrderId(orderId);
        orderMonitorRecord.setFileUrl(url);
        orderMonitorRecord.setContent(orderMonitorForm.getContent());

        Result<TextAuditingVo> textAuditingVoResult = ciFeignClient.textAuditing(orderMonitorForm.getContent());
        textAuditingVoResult.throwOnFailureOrDataIsNull();
        //记录审核结果
        TextAuditingVo textAuditingVo = textAuditingVoResult.getData();
        orderMonitorRecord.setResult(textAuditingVo.getResult());
        orderMonitorRecord.setKeywords(textAuditingVo.getKeywords());

        Result<Boolean> result = orderMonitorFeignClient.saveMonitorRecord(orderMonitorRecord);
        result.throwOnFailureOrDataIsNull();

        Result<OrderMonitor> orderMonitorResult = orderMonitorFeignClient.getOrderMonitor(orderMonitorForm.getOrderId());
        orderMonitorResult.throwOnFailureOrDataIsNull();
        OrderMonitor orderMonitor = orderMonitorResult.getData();
        //更新订单监控统计
        int fileNum = orderMonitor.getFileNum() + 1;
        orderMonitor.setFileNum(fileNum);
        //审核结果: 0（审核正常），1 （判定为违规敏感文件），2（疑似敏感，建议人工复核）。
        if("2".equals(orderMonitorRecord.getResult())) {
            int auditNum = orderMonitor.getAuditNum() + 1;
            orderMonitor.setAuditNum(auditNum);
        }
        orderMonitorFeignClient.updateOrderMonitor(orderMonitor);


        return result.getData();
    }
}
