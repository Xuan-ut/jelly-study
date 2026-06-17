package com.jellystudy.companion.ai.prompt;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 群体智慧蜂巢 Prompt 构建器
 */
@Component
public class HivePromptBuilder {

    public String buildPatternDiscoveryPrompt(String subject, String dataSummary) {
        return String.format(
                "请分析以下学科的用户学习行为数据，发现共性学习规律。\n\n" +
                "学科：%s\n" +
                "汇总数据：\n" +
                "%s\n\n" +
                "请从以下5种模式类型中识别发现：\n" +
                "1. BOTTLENECK（瓶颈模式）：某知识点在特定时间点遇到困难\n" +
                "2. PATH（路径模式）：最优学习顺序发现\n" +
                "3. ABANDONMENT（放弃模式）：早期放弃风险识别\n" +
                "4. BREAKTHROUGH（突破模式）：有效学习方法发现\n" +
                "5. FORGETTING（遗忘模式）：遗忘规律发现\n\n" +
                "对于每个发现的模式，请说明：\n" +
                "- 模式类型\n" +
                "- 描述\n" +
                "- 统计数据（影响比例、平均卡住天数等）\n" +
                "- 建议的干预措施\n" +
                "- 置信度（0.0-1.0）\n\n" +
                "只返回发现的模式，每个模式用分隔线'---'隔开。如果没有发现高置信度的模式，返回\"未发现新模式\"。",
                subject, dataSummary
        );
    }

    public String buildAnomalyDetectionPrompt(Long userId,
                                              Map<String, Object> userStats,
                                              Map<String, Object> populationStats) {
        return String.format(
                "请对比以下用户的学习数据与群体平均水平，识别异常信号。\n\n" +
                "用户数据：%s\n" +
                "群体平均数据：%s\n\n" +
                "请从以下维度评估：\n" +
                "- 日均学习时长\n" +
                "- 任务完成率\n" +
                "- QA提问频率\n" +
                "- 知识点掌握速度\n" +
                "- 连续未学习天数\n\n" +
                "对每个维度给出：\n" +
                "- 用户值 vs 正常范围\n" +
                "- 状态（NORMAL/WARNING/CRITICAL）\n" +
                "- 个性化建议\n\n" +
                "最后给出综合风险等级和总体建议。",
                userStats, populationStats
        );
    }

    public String buildPathRecommendationPrompt(Long userId, String subject,
                                                  List<Map<String, Object>> successfulPaths) {
        return String.format(
                "请基于成功用户的学习路径数据，为当前用户推荐最优学习路径。\n\n" +
                "当前学习学科：%s\n" +
                "可选学习路径（基于成功用户数据）：%s\n\n" +
                "请对每条路径评估：\n" +
                "- 成功率\n" +
                "- 平均完成天数\n" +
                "- 适合的学习者类型\n" +
                "- 是否推荐（标记一条为推荐路径）\n" +
                "- 风险提示（如果有）\n\n" +
                "按成功率从高到低排列。标记一条最适合的路径为\"推荐\"。",
                subject, successfulPaths
        );
    }
}
