package com.wrongnote.service;

import com.aliyun.oss.OSS;
import com.wrongnote.config.OssConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class OssService {

    @Autowired(required = false)
    private OSS ossClient;

    @Autowired
    private OssConfig ossConfig;

    public String uploadImage(MultipartFile file) throws IOException {
        if (ossClient == null) {
            throw new RuntimeException("OSS 未配置，请在 application.yml 中配置 aliyun.oss.access-key-id");
        }
        String fileName = "notes/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        ossClient.putObject(ossConfig.getBucketName(), fileName, file.getInputStream());
        return ossConfig.getUrlPrefix() + fileName;
    }
}
