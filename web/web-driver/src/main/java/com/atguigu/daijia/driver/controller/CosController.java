package com.atguigu.daijia.driver.controller;

import com.atguigu.daijia.common.auth.KjyLogin;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Tag(name = "腾讯云cos上传接口管理")
@RestController
@RequestMapping(value="/cos")
public class CosController {


    @Resource
    private CosService cosService;

    //文件上传接口
    @Operation(summary = "上传文件(身份证/驾驶证/人脸~)~")
    @KjyLogin
    @PostMapping("/upload")
    public Result<CosUploadVo> upload(@RequestPart("file") MultipartFile file,
                                      @RequestParam(name = "path",defaultValue = "auth") String type) {
        CosUploadVo cosUploadVo = cosService.uploadFile(file,type);
        return Result.ok(cosUploadVo);
    }

}

