package com.wrongnote.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class OssConfig {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucket-name:wrong-note}")
    private String bucketName;

    @Value("${aliyun.oss.url-prefix:}")
    private String urlPrefix;

    @Bean
    public OSS ossClient() {
        if (accessKeyId == null || accessKeyId.isEmpty()) {
            return null;
        }
        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }
}

