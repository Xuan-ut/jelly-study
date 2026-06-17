package com.jellystudy.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.companion.cache")
public class CacheConfigProperties {
    private int spiritTtl = 30;
    private int treeTtl = 60;
    private int patternTtl = 360;
}
