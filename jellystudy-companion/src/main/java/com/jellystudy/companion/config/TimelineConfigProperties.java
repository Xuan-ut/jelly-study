package com.jellystudy.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.companion.timeline")
public class TimelineConfigProperties {
    private double forgettingBaseRate = 0.3;
    private double forgettingStabilityFactor = 0.1;
    private int predictionDays = 90;
}
