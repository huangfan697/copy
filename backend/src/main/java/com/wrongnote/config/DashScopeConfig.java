package com.wrongnote.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class DashScopeConfig {

    @Value("${dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${dashscope.api-key:}")
    private String apiKey;

    @Value("${dashscope.model:qwen-vl-plus}")
    private String model;
}
