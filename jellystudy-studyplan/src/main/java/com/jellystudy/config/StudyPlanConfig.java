package com.jellystudy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.studyplan")
public class StudyPlanConfig {
    private int knowledgePointsPerStage = 3;
    private int defaultStudyDuration = 30;
    private int maxActivePlans = 5;
    private boolean achievementEnabled = true;
    private String welcomeMessage = "欢迎使用JellyStudy学习计划";
}
