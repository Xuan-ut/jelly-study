package com.jellystudy.companion.ai.prompt;

import com.jellystudy.companion.ai.model.AssessmentResult;
import com.jellystudy.companion.config.FeynmanConfigProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 费曼反转教学 Prompt 构建器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class FeynmanPromptBuilder {

    private final FeynmanConfigProperties feynmanConfig;

    public String buildFirstQuestionPrompt(String knowledgeName, String knowledgeContent) {
        return String.format(
                "%s\n\n" +
                "知识点：%s\n" +
                "内容：%s\n\n" +
                "请生成第一个问题，引导用户向你解释这个知识点。\n" +
                "要求：像好奇的学生一样提问，不要说\"请解释\"这样的学术用语，\n" +
                "而是用\"我正在学XX，你能教教我吗？\"这样的自然表达。\n" +
                "只返回问题本身，不要加引号或其他说明。",
                feynmanConfig.getQuestionPrompt().replace("{knowledge}", knowledgeName),
                knowledgeName, knowledgeContent
        );
    }

    public String buildFollowUpPrompt(String knowledgeName, String userExplanation,
                                       AssessmentResult lastAssessment) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
                "你是一个聪明的学生，正在学习\"%s\"。用户的解释是：\n" +
                "\"%s\"\n\n" +
                "上次评估结果：\n" +
                "- 总体理解度：%.0f%%\n",
                knowledgeName, userExplanation,
                lastAssessment.getOverallScore() * 100));

        if (!lastAssessment.getMissingPoints().isEmpty()) {
            sb.append("- 遗漏的要点：").append(String.join("、", lastAssessment.getMissingPoints())).append("\n");
        }
        if (!lastAssessment.getMisconceptions().isEmpty()) {
            sb.append("- 可能错误的理解：").append(String.join("、", lastAssessment.getMisconceptions())).append("\n");
        }

        sb.append("\n请生成一个追问，要求：\n")
          .append("1. 针对遗漏的要点或可能错误的理解进行追问\n")
          .append("2. 如果用户解释很好（理解度>80%），可以挑战更深层次的问题\n")
          .append("3. 语气保持好奇、友善，像一个真正想学习的学生\n")
          .append("4. 只返回问题，不要加标签或说明");
        return sb.toString();
    }

    public String buildAssessmentPrompt(String knowledgeContent, String userExplanation) {
        return String.format(
                "请评估用户对以下知识点的解释质量。\n\n" +
                "知识点标准内容：%s\n" +
                "用户的解释：%s\n\n" +
                "请从以下5个维度分别评分（0.0-1.0）：\n" +
                "- accuracy: 概念是否准确\n" +
                "- completeness: 要点是否完整\n" +
                "- depth: 理解是否深入（是否涉及底层原理）\n" +
                "- clarity: 表达是否清晰（是否用自己的话）\n" +
                "- abilityToExample: 能否举例说明\n\n" +
                "同时输出：\n" +
                "- overallScore: 综合评分\n" +
                "- missingPoints: 遗漏的知识要点（数组）\n" +
                "- misconceptions: 错误理解（数组）\n" +
                "- suggestedNextStep: 下一步学习建议\n\n" +
                "请严格返回以下JSON格式（不要包含其他文字）：\n" +
                "{\n" +
                "  \"overallScore\": 0.0,\n" +
                "  \"dimensions\": {\n" +
                "    \"accuracy\": 0.0,\n" +
                "    \"completeness\": 0.0,\n" +
                "    \"depth\": 0.0,\n" +
                "    \"clarity\": 0.0,\n" +
                "    \"abilityToExample\": 0.0\n" +
                "  },\n" +
                "  \"missingPoints\": [],\n" +
                "  \"misconceptions\": [],\n" +
                "  \"recommendedReview\": [],\n" +
                "  \"suggestedNextStep\": \"\"\n" +
                "}",
                knowledgeContent, userExplanation
        );
    }
}
