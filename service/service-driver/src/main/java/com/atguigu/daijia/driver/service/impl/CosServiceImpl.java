package com.atguigu.daijia.driver.service.impl;

import cn.hutool.core.date.DateUtil;
import com.atguigu.daijia.common.execption.GuiguException;
import com.atguigu.daijia.common.result.ResultCodeEnum;
import com.atguigu.daijia.driver.service.CiService;
import com.atguigu.daijia.driver.service.CosService;
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
public class CosServiceImpl implements CosService {

    // 注入实列
    @Resource
    private FileStorageService fileStorageService;

    @Resource
    private CiService ciService;

    @Override
    public CosUploadVo upload(MultipartFile file, String type) {
        FileInfo fileInfo;
        String objectName = String.format("%s/%s/", LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")), type);
        try {
            // 指定oss保存文件路径
            // 上传图片，成功返回文件信息
            fileInfo = fileStorageService.of(file).setPath(objectName).upload();

            String uploadPath = fileInfo.getPath() + fileInfo.getFilename();
            // 审核图片
            Boolean isAuditing = ciService.imageAuditing(uploadPath);
            if(Boolean.FALSE.equals(isAuditing)) {
                //删除违规图片
                fileStorageService.delete(uploadPath);
                throw new GuiguException(ResultCodeEnum.IMAGE_AUDITION_FAIL);
            }

        } catch (Exception e) {
            log.error("CosServiceImpl upload 上传文件={},失败原因={}", file.getName(), e.getMessage());
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
                // .putResponseHeaders(
                //         // 设置一个响应头，将下载时的文件名改成 NewDownloadFileName.jpg，不需要可省略
                //         // 这里也可以设置其它的想要的响应头，每个存储平台支持情况都不太相同，可以自行测试或查询相关文档
                //         Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=NewDownloadFileName.jpg")
                .generatePresignedUrl();


        return downloadResult.getUrl();

    }
    @Override
    public String getImageUrl(String path) {


        // 生成下载或访问用的 URL
        GeneratePresignedUrlResult downloadResult = fileStorageService
                .generatePresignedUrl()
                .setPlatform(fileStorageService.getProperties().getDefaultPlatform()) // 存储平台，不传使用默认的
                .setPath(path) // 文件路径  例如  2025/03/19/test/67da3919443cb52a408af980.png
                // .setFilename(fileInfo.getFilename()) // 文件名，也可以换成缩略图的文件名
                .setMethod(Constant.GeneratePresignedUrl.Method.GET) // 签名方法
                .setExpiration(DateUtil.offsetMinute(new Date(), 15)) // 过期时间 15 分钟
                // .putResponseHeaders(
                //         // 设置一个响应头，将下载时的文件名改成 NewDownloadFileName.jpg，不需要可省略
                //         // 这里也可以设置其它的想要的响应头，每个存储平台支持情况都不太相同，可以自行测试或查询相关文档
                //         Constant.Metadata.CONTENT_DISPOSITION, "attachment;filename=NewDownloadFileName.jpg")
                .generatePresignedUrl();


        return downloadResult.getUrl();

    }
}
