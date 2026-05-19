package com.wrongnote.service;

import com.aliyun.oss.OSS;
import com.wrongnote.config.OssConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
public class OssService {

    @Autowired(required = false)
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    public String uploadImage(MultipartFile file) throws IOException {
        if (ossClient != null) {
            String fileName = "notes/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            ossClient.putObject(ossConfig.getBucketName(), fileName, file.getInputStream());
            String url = ossConfig.getUrlPrefix() + fileName;
            log.info("图片上传到 OSS: {}", url);
            return url;
        }

        // OSS 未配置，转 base64 data URI 返回
        String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
        String base64 = Base64.getEncoder().encodeToString(file.getBytes());
        String dataUri = "data:" + mimeType + ";base64," + base64;
        log.info("OSS 未配置，图片转为 base64 data URI (长度: {})", dataUri.length());
        return dataUri;
    }
}
