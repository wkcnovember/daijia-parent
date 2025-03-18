package com.atguigu.daijia.driver.service.impl;

import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.Result;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.client.CosFeignClient;
import com.atguigu.daijia.driver.service.CosService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Objects;

@Slf4j
@Service
@SuppressWarnings({"unchecked", "rawtypes"})
public class CosServiceImpl implements CosService {

    @Resource
    private CosFeignClient cosFeignClient;



    @Override
    public CosUploadVo uploadFile(MultipartFile file) {
        //远程调用
        Result<CosUploadVo> cosUploadVoResult = cosFeignClient.upload(file);
        if(!Objects.equals(cosUploadVoResult.getCode(), ResultCodeEnum.SUCCESS.getCode())) {
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        return cosUploadVoResult.getData();
    }
}
