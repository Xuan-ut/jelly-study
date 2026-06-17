package com.jellystudy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.knowledge")
public class KnowledgeConfig {

    private int defaultPageSize = 10;

    private int maxNameLength = 50;

    private boolean autoCreateEnabled = true;

    private String welcomeMessage = "欢迎使用JellyStudy知识点管理";
}
