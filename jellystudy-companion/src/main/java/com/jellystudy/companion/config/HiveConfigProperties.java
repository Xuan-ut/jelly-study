package com.jellystudy.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.companion.hive")
public class HiveConfigProperties {
    private int minSampleSize = 100;
    private double confidenceThreshold = 0.8;
    private int anomalyStudyTimeLow = 15;
    private double anomalyCompletionRateLow = 0.4;
    private int anomalyInactiveDays = 2;
}
