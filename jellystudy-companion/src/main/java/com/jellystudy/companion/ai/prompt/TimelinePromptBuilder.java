package com.jellystudy.companion.ai.prompt;

import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import org.springframework.stereotype.Component;

/**
 * 时空预测 Prompt 构建器
 */
@Component
public class TimelinePromptBuilder {

    public String buildPredictionPrompt(KnowledgeTreeSnapshot current, boolean optimistic, int daysAhead) {
        String scenario = optimistic ? "用户保持当前学习节奏继续学习" : "用户从现在开始完全停止学习";
        return String.format(
                "请根据以下知识树数据，生成一段预测描述（50字以内）。\n\n" +
                "当前知识树状态：\n" +
                "总分枝数：%d\n" +
                "平均掌握度：%.0f%%\n\n" +
                "场景假设：%s\n" +
                "预测天数：%d天后\n\n" +
                "请用生动形象的语言描述这个场景下的知识树状态，\n" +
                "乐观预测要激励人心，悲观预测要有紧迫感但不过分恐吓。\n" +
                "只返回描述文本。",
                current.getBranches().size(),
                current.getBranches().stream()
                        .mapToDouble(KnowledgeTreeSnapshot.Branch::getProgress)
                        .average().orElse(0) * 100,
                scenario,
                daysAhead
        );
    }
}
