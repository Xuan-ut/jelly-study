package com.jellystudy.companion.service.hive;

import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.repository.AnomalyRecordRepository;
import com.jellystudy.companion.repository.SpiritStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 学习健康报告服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class HealthReportServiceImpl implements HealthReportService {

    private final AnomalyDetectionService anomalyDetectionService;
    private final SpiritStateRepository spiritStateRepository;
    private final AnomalyRecordRepository anomalyRecordRepository;

    @Override
    public Map<String, Object> generateHealthReport(Long userId) {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("userId", userId);
        report.put("reportDate", java.time.LocalDate.now().toString());

        // 精灵状态
        SpiritState spirit = spiritStateRepository.findByUserId(userId).orElse(null);
        if (spirit != null) {
            report.put("spiritLevel", spirit.getLevelName());
            report.put("spiritEmotion", spirit.getEmotion().getDisplayName());
            report.put("spiritExperience", spirit.getExperience());
            report.put("spiritSatiation", spirit.getSatiation());
        }

        // 异常检测
        AnomalyRecord anomaly = anomalyDetectionService.detectAnomalies(userId);
        report.put("overallRisk", anomaly.getOverallRisk());
        report.put("overallRiskLevel", anomaly.getOverallRiskLevel());

        if (anomaly.getDimensions() != null) {
            List<Map<String, Object>> dims = new ArrayList<>();
            for (AnomalyRecord.DimensionResult d : anomaly.getDimensions()) {
                Map<String, Object> dim = new LinkedHashMap<>();
                dim.put("name", d.getName());
                dim.put("userValue", d.getUserValue());
                dim.put("normalRange", d.getNormalRange());
                dim.put("status", d.getStatus());
                dim.put("recommendation", d.getRecommendation());
                dims.add(dim);
            }
            report.put("dimensions", dims);
        }

        // 健康分数
        double healthScore = calculateHealthScore(spirit, anomaly);
        report.put("healthScore", Math.round(healthScore * 100.0) / 100.0);

        // 建议
        List<String> suggestions = new ArrayList<>();
        if (healthScore >= 80) {
            suggestions.add("学习状态良好，继续保持！");
        } else if (healthScore >= 60) {
            suggestions.add("学习状态一般，可以适当增加学习时间");
        } else {
            suggestions.add("学习状态需要关注，建议每天坚持学习至少15分钟");
        }
        report.put("suggestions", suggestions);

        return report;
    }

    private double calculateHealthScore(SpiritState spirit, AnomalyRecord anomaly) {
        double score = 70.0; // 基础分

        if (spirit != null) {
            score += spirit.getSatiation() * 0.2; // 饱食度贡献
            score += spirit.getLevel() * 2.0; // 等级贡献
        }

        // 异常维度扣分
        if (anomaly.getDimensions() != null) {
            for (AnomalyRecord.DimensionResult d : anomaly.getDimensions()) {
                if ("CRITICAL".equals(d.getStatus())) {
                    score -= 15;
                } else if ("WARNING".equals(d.getStatus())) {
                    score -= 5;
                }
            }
        }

        return Math.max(0, Math.min(100, score));
    }
}
