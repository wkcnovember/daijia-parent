package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.date.DateUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.service.FileService;
import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.x.file.storage.core.FileInfo;
import org.dromara.x.file.storage.core.FileStorageService;
import org.dromara.x.file.storage.core.constant.Constant;
import org.dromara.x.file.storage.core.presigned.GeneratePresignedUrlResult;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;

@Slf4j
@Service
public class FileServiceImpl implements FileService {
    // @Resource
    // private CosFeignClient cosFeignClient;

    @Resource
    private FileStorageService fileStorageService;


    @Override
    public CosUploadVo upload(MultipartFile file) {


        FileInfo fileInfo;
        String objectName = String.format("%s/", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")));
        try {
            // 指定oss保存文件路径
            // 上传图片，成功返回文件信息
            fileInfo = fileStorageService
                    .of(file)
                    .setPlatform("minio-1")
                    .setPath(objectName).upload();

        } catch (Exception e) {
            log.error("FileService upload 上传文件={},失败原因={}", file.getName(), e.getMessage());
            throw new GuiguException(ResultCodeEnum.DATA_ERROR);
        }
        // 返回vo对象
        CosUploadVo cosUploadVo = new CosUploadVo();

        cosUploadVo.setUrl(fileInfo.getPath() + fileInfo.getFilename());
        //  图片临时访问url，回显使用
        cosUploadVo.setShowUrl(getShowUrl(fileInfo));
        return cosUploadVo;
    }

    private String getShowUrl(FileInfo fileInfo) {

        // 生成下载或访问用的 URL
        GeneratePresignedUrlResult downloadResult = fileStorageService
                .generatePresignedUrl()
                .setPlatform(fileInfo.getPlatform()) // 存储平台，不传使用默认的
                .setPath(fileInfo.getPath()) // 文件路径
                .setFilename(fileInfo.getFilename()) // 文件名，也可以换成缩略图的文件名
                .setMethod(Constant.GeneratePresignedUrl.Method.GET) // 签名方法
                .setExpiration(DateUtil.offsetMinute(new Date(), 15)) // 过期时间 15 分钟
                .generatePresignedUrl();

        return downloadResult.getUrl();

    }
}
