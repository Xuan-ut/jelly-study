package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.config.HiveConfigProperties;
import com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer;
import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.AnomalyRecord.DimensionResult;
import com.jellystudy.companion.repository.AnomalyRecordRepository;
import com.jellystudy.companion.util.AnomalyScoreUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 个体异常检测服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AnomalyDetectionServiceImpl implements AnomalyDetectionService {

    private final AnomalyRecordRepository anomalyRecordRepository;
    private final StudyPlanServiceConsumer studyPlanServiceConsumer;
    private final HiveConfigProperties hiveConfig;

    @Override
    public AnomalyRecord detectAnomalies(Long userId) {
        return generateReport(userId);
    }

    @Override
    public AnomalyRecord generateReport(Long userId) {
        List<DimensionResult> dimensions = new ArrayList<>();

        // 维度1: 日均学习时长
        Map<String, Object> stats = studyPlanServiceConsumer.getUserStudyStats(userId);
        double studyTime = getStudyTimeFromStats(stats);
        AnomalyScoreUtil.AnomalyResult timeResult = AnomalyScoreUtil.assess(
                studyTime, 45.0, 15.0, "日均学习时长");
        dimensions.add(DimensionResult.builder()
                .name(timeResult.getName())
                .userValue(timeResult.getUserValue())
                .normalRange(timeResult.getNormalRange())
                .status(timeResult.getStatus())
                .recommendation(timeResult.getRecommendation())
                .build());

        // 维度2: 任务完成率
        double completionRate = getCompletionRateFromStats(stats);
        AnomalyScoreUtil.AnomalyResult compResult = AnomalyScoreUtil.assess(
                completionRate, 0.7, 0.2, "任务完成率");
        dimensions.add(DimensionResult.builder()
                .name(compResult.getName())
                .userValue(compResult.getUserValue())
                .normalRange(compResult.getNormalRange())
                .status(compResult.getStatus())
                .recommendation(compResult.getRecommendation())
                .build());

        // 综合评估
        String overallRisk = dimensions.stream().anyMatch(d -> "CRITICAL".equals(d.getStatus()))
                ? "高风险" : dimensions.stream().anyMatch(d -> "WARNING".equals(d.getStatus()))
                ? "中等风险" : "正常";
        String overallRiskLevel = dimensions.stream().anyMatch(d -> "CRITICAL".equals(d.getStatus()))
                ? "HIGH" : dimensions.stream().anyMatch(d -> "WARNING".equals(d.getStatus()))
                ? "MEDIUM" : "LOW";

        AnomalyRecord record = AnomalyRecord.builder()
                .userId(userId)
                .reportDate(LocalDate.now())
                .dimensions(dimensions)
                .overallRisk(overallRisk)
                .overallRiskLevel(overallRiskLevel)
                .created(LocalDate.now())
                .build();

        return anomalyRecordRepository.save(record);
    }

    private double getStudyTimeFromStats(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) return 0;
        int totalStages = ((Number) stats.getOrDefault("totalStages", 0)).intValue();
        int completedStages = ((Number) stats.getOrDefault("completedStages", 0)).intValue();
        // 每个阶段估算30分钟
        return completedStages * 30.0 / Math.max(1, totalStages > 0 ? totalStages : 1);
    }

    private double getCompletionRateFromStats(Map<String, Object> stats) {
        if (stats == null || stats.isEmpty()) return 0;
        int totalStages = ((Number) stats.getOrDefault("totalStages", 0)).intValue();
        int completedStages = ((Number) stats.getOrDefault("completedStages", 0)).intValue();
        return totalStages > 0 ? (double) completedStages / totalStages : 0;
    }
}
