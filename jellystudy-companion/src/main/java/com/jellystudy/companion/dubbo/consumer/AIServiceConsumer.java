package com.jellystudy.companion.dubbo.consumer;

import com.jellystudy.dubbo.AIDubboService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * AI 服务 Dubbo Consumer（作为降级备选通道）
 */
@Component
@Slf4j
public class AIServiceConsumer {

    @DubboReference(version = "1.0.0", check = false)
    private AIDubboService aiDubboService;

    /**
     * 调用AI分析薄弱点（降级通道）
     */
    public String analyzeWeakPoints(Long userId, List<Map<String, Object>> studyRecords) {
        try {
            return aiDubboService.analyzeWeakPoints(userId, studyRecords);
        } catch (Exception e) {
            log.error("调用AI服务失败: analyzeWeakPoints, userId={}, error={}", userId, e.getMessage());
            return "暂时无法分析薄弱点";
        }
    }

    /**
     * 调用AI生成每日计划（降级通道）
     */
    public String generateDailyPlan(Long userId, String planId, List<Map<String, Object>> recentProgress) {
        try {
            return aiDubboService.generateDailyPlan(userId, planId, recentProgress);
        } catch (Exception e) {
            log.error("调用AI服务失败: generateDailyPlan, userId={}, error={}", userId, e.getMessage());
            return "暂时无法生成每日计划";
        }
    }

    /**
     * 调用AI生成学习计划详情（降级通道）
     */
    public String generatePlanDetail(String subject, String goal, int stageCount, String difficulty) {
        try {
            return aiDubboService.generatePlanDetail(subject, goal, stageCount, difficulty);
        } catch (Exception e) {
            log.error("调用AI服务失败: generatePlanDetail, subject={}, error={}", subject, e.getMessage());
            return null;
        }
    }

    /**
     * 调用AI分析用户行为（降级通道）
     */
    public String analyzeUserBehavior(Long userId, List<Map<String, Object>> activities) {
        try {
            return aiDubboService.analyzeUserBehavior(userId, activities);
        } catch (Exception e) {
            log.error("调用AI服务失败: analyzeUserBehavior, userId={}, error={}", userId, e.getMessage());
            return "暂时无法分析用户行为";
        }
    }
}
