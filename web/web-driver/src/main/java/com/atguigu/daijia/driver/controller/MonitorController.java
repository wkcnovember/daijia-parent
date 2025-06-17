package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.auth.KjyLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.MonitorService;
import com.atguigu.daijia.model.form.order.OrderMonitorForm;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "监控接口管理")
@RestController
@RequestMapping(value = "/monitor")
public class MonitorController {

    @Resource
    private MonitorService monitorService;

    @Operation(summary = "微信同声传译上传录音")
    @PostMapping("/upload")
    @KjyLogin
    public Result<Boolean> upload(@RequestPart("file") MultipartFile file,
                                  OrderMonitorForm orderMonitorForm) {

        return Result.ok(monitorService.upload(file, orderMonitorForm));
    }


}

