package com.jellystudy.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.companion.feynman")
public class FeynmanConfigProperties {
    private int maxRounds = 5;
    private double passThreshold = 0.85;
    private double failThreshold = 0.5;
    private String questionPrompt;
}
