package com.atguigu.daijia.driver.service;

import com.atguigu.daijia.model.vo.driver.CosUploadVo;
import org.springframework.web.multipart.MultipartFile;

public interface CosService {


    CosUploadVo upload(MultipartFile file, String type);

    // 根据文件path生成临时访问url
    String getImageUrl(String fileInfo);
}
