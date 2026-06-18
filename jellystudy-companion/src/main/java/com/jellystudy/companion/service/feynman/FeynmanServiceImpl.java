package com.jellystudy.companion.service.feynman;

import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.config.FeynmanConfigProperties;
import com.jellystudy.companion.dubbo.consumer.KnowledgeServiceConsumer;
import com.jellystudy.companion.entity.FeynmanSession;
import com.jellystudy.companion.entity.FeynmanSession.FinalAssessment;
import com.jellystudy.companion.entity.FeynmanSession.Round;
import com.jellystudy.companion.entity.FeynmanSession.RoundAssessment;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.companion.exception.CompanionBusinessException;
import com.jellystudy.companion.exception.ErrorCode;
import com.jellystudy.companion.repository.FeynmanSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 费曼反转教学主服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeynmanServiceImpl implements FeynmanService {

    private final FeynmanSessionRepository sessionRepository;
    private final FeynmanAssessmentService assessmentService;
    private final FeynmanQuestionService questionService;
    private final KnowledgeServiceConsumer knowledgeServiceConsumer;
    private final FeynmanConfigProperties feynmanConfig;
    private final AIClient aiClient;

    @Override
    public FeynmanSessionResult startSession(Long userId, String knowledgeId) {
        KnowledgePoint kp = knowledgeServiceConsumer.getKnowledgeDetail(knowledgeId);
        String knowledgeName = kp != null ? kp.getName() : knowledgeId;
        String knowledgeContent = kp != null ? kp.getDescription() : "";

        // 生成第一个问题
        String aiQuestion = questionService.generateFirstQuestion(knowledgeName, knowledgeContent);

        String sessionId = UUID.randomUUID().toString().substring(0, 8);
        FeynmanSession session = FeynmanSession.builder()
                .userId(userId)
                .sessionId(sessionId)
                .knowledgeId(knowledgeId)
                .knowledgeName(knowledgeName)
                .status("IN_PROGRESS")
                .rounds(new ArrayList<>())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        sessionRepository.save(session);

        log.info("费曼教学会话开始: userId={}, knowledgeId={}, sessionId={}", userId, knowledgeId, sessionId);
        return new FeynmanSessionResult(sessionId, knowledgeId, knowledgeName, aiQuestion, "IN_PROGRESS", 0);
    }

    @Override
    public FeynmanRespondResult respond(String sessionId, String userExplanation) {
        FeynmanSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CompanionBusinessException(ErrorCode.FEYNMAN_SESSION_NOT_FOUND));

        // 检查最大轮数
        if (session.getRounds().size() >= feynmanConfig.getMaxRounds()) {
            // 强制结束会话
            concludeSession(session);
            return buildResult(session, null, "我们已经讨论了很多啦，今天先到这里吧！", true);
        }

        // 获取知识点内容用于评估
        KnowledgePoint kp = knowledgeServiceConsumer.getKnowledgeDetail(session.getKnowledgeId());
        String knowledgeContent = kp != null ? kp.getDescription() : "";

        // 评估用户解释
        com.jellystudy.companion.ai.model.AssessmentResult assessment = assessmentService.assess(knowledgeContent, userExplanation);

        // 保存本轮记录
        Round round = Round.builder()
                .roundNumber(session.getRounds().size() + 1)
                .userExplanation(userExplanation)
                .aiQuestion("") // 将在下一轮或结束时填充
                .assessment(RoundAssessment.builder()
                        .accuracy(assessment.getDimensions().getOrDefault("accuracy", 0.5))
                        .completeness(assessment.getDimensions().getOrDefault("completeness", 0.5))
                        .depth(assessment.getDimensions().getOrDefault("depth", 0.5))
                        .clarity(assessment.getDimensions().getOrDefault("clarity", 0.5))
                        .abilityToExample(assessment.getDimensions().getOrDefault("abilityToExample", 0.5))
                        .build())
                .build();
        session.getRounds().add(round);

        boolean sessionComplete = false;
        String aiQuestion;
        String spiritReaction;

        if (assessment.getOverallScore() >= feynmanConfig.getPassThreshold()) {
            // 用户理解达标，结束会话
            sessionComplete = true;
            aiQuestion = null;
            concludeSession(session);
            spiritReaction = "太厉害了！你解释得比书上清楚多了！我现在完全理解了！";
        } else if (assessment.getOverallScore() < feynmanConfig.getFailThreshold()) {
            // 理解度太低，温和提示
            aiQuestion = questionService.generateFollowUpQuestion(
                    session.getKnowledgeName(), userExplanation, assessment);
            spiritReaction = "嗯...这部分我还有点不太明白，我们换个角度试试？";
        } else {
            // 正常追问
            aiQuestion = questionService.generateFollowUpQuestion(
                    session.getKnowledgeName(), userExplanation, assessment);
            spiritReaction = "我明白了大部分，不过还有一个疑问...";
        }

        round.setAiQuestion(aiQuestion != null ? aiQuestion : "");
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);

        return buildResult(session, aiQuestion, spiritReaction, sessionComplete);
    }

    @Override
    public AssessmentResult getAssessment(String sessionId) {
        FeynmanSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CompanionBusinessException(ErrorCode.FEYNMAN_SESSION_NOT_FOUND));

        FeynmanService.AssessmentResult result = new FeynmanService.AssessmentResult()
                .sessionId(session.getSessionId())
                .knowledgeId(session.getKnowledgeId());

        if (session.getFinalAssessment() != null) {
            result.overallScore(session.getFinalAssessment().getOverallScore())
                    .missingPoints(session.getFinalAssessment().getMissingPoints())
                    .misconceptions(session.getFinalAssessment().getMisconceptions())
                    .recommendedReview(session.getFinalAssessment().getRecommendedReview())
                    .suggestedNextStep(session.getFinalAssessment().getSuggestedNextStep());
        }
        return result;
    }

    @Override
    public AssessmentResult endSession(String sessionId) {
        FeynmanSession session = sessionRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new CompanionBusinessException(ErrorCode.FEYNMAN_SESSION_NOT_FOUND));

        // 如果已经结束过，直接返回已有评估
        if ("COMPLETED".equals(session.getStatus()) && session.getFinalAssessment() != null) {
            return new AssessmentResult()
                    .sessionId(session.getSessionId())
                    .knowledgeId(session.getKnowledgeId())
                    .overallScore(session.getFinalAssessment().getOverallScore())
                    .missingPoints(session.getFinalAssessment().getMissingPoints())
                    .misconceptions(session.getFinalAssessment().getMisconceptions())
                    .recommendedReview(session.getFinalAssessment().getRecommendedReview())
                    .suggestedNextStep(session.getFinalAssessment().getSuggestedNextStep());
        }

        // 生成详细的教学总结
        concludeSessionWithDetails(session);
        session.setUpdateTime(LocalDateTime.now());
        sessionRepository.save(session);

        return new AssessmentResult()
                .sessionId(session.getSessionId())
                .knowledgeId(session.getKnowledgeId())
                .overallScore(session.getFinalAssessment().getOverallScore())
                .missingPoints(session.getFinalAssessment().getMissingPoints())
                .misconceptions(session.getFinalAssessment().getMisconceptions())
                .recommendedReview(session.getFinalAssessment().getRecommendedReview())
                .suggestedNextStep(session.getFinalAssessment().getSuggestedNextStep());
    }

    private void concludeSession(FeynmanSession session) {
        concludeSessionWithDetails(session);
    }

    /** 生成详细的教学总结，使用AI分析对话内容 */
    private void concludeSessionWithDetails(FeynmanSession session) {
        session.setStatus("COMPLETED");

        // 计算各维度平均分
        double totalScore = 0;
        double totalAccuracy = 0, totalCompleteness = 0, totalDepth = 0, totalClarity = 0, totalAbilityToExample = 0;
        int roundCount = session.getRounds().size();

        if (roundCount > 0) {
            for (Round r : session.getRounds()) {
                RoundAssessment a = r.getAssessment();
                totalAccuracy += a.getAccuracy();
                totalCompleteness += a.getCompleteness();
                totalDepth += a.getDepth();
                totalClarity += a.getClarity();
                totalAbilityToExample += a.getAbilityToExample();
                totalScore += (a.getAccuracy() + a.getCompleteness() + a.getDepth()
                        + a.getClarity() + a.getAbilityToExample()) / 5.0;
            }
            totalScore /= roundCount;
            totalAccuracy /= roundCount;
            totalCompleteness /= roundCount;
            totalDepth /= roundCount;
            totalClarity /= roundCount;
            totalAbilityToExample /= roundCount;
        }

        // 构建对话摘要给AI分析
        StringBuilder dialogSummary = new StringBuilder();
        dialogSummary.append("知识点：").append(session.getKnowledgeName()).append("\n");
        dialogSummary.append("共进行了").append(roundCount).append("轮教学对话：\n");
        for (int i = 0; i < session.getRounds().size(); i++) {
            Round r = session.getRounds().get(i);
            RoundAssessment a = r.getAssessment();
            dialogSummary.append("第").append(i + 1).append("轮：\n");
            dialogSummary.append("  用户解释：").append(r.getUserExplanation()).append("\n");
            dialogSummary.append("  评分 - 准确性:").append(String.format("%.0f%%", a.getAccuracy() * 100))
                    .append(" 完整性:").append(String.format("%.0f%%", a.getCompleteness() * 100))
                    .append(" 深度:").append(String.format("%.0f%%", a.getDepth() * 100))
                    .append(" 清晰度:").append(String.format("%.0f%%", a.getClarity() * 100))
                    .append(" 举例能力:").append(String.format("%.0f%%", a.getAbilityToExample() * 100)).append("\n");
        }
        dialogSummary.append("综合得分：").append(String.format("%.0f%%", totalScore * 100));

        // 使用AI生成教学总结
        List<String> missingPoints = new ArrayList<>();
        List<String> misconceptions = new ArrayList<>();
        List<String> recommendedReview = new ArrayList<>();
        String suggestedNextStep;

        try {
            String aiPrompt = String.format(
                    "你是一位专业的学习分析师。请根据以下费曼反转教学对话记录，生成详细的教学质量分析报告。\n\n" +
                    "%s\n\n" +
                    "请严格按以下JSON格式输出（不要markdown代码块、不要注释、不要省略号）：\n" +
                    "{\"missingPoints\":[\"遗漏点1\",\"遗漏点2\"],\"misconceptions\":[\"误解1\"],\"recommendedReview\":[\"建议1\",\"建议2\"],\"suggestedNextStep\":\"下一步建议\"}\n\n" +
                    "要求：\n" +
                    "1. missingPoints：用户在解释中遗漏的关键知识点（2-3条）\n" +
                    "2. misconceptions：用户可能存在的概念误解（1-2条）\n" +
                    "3. recommendedReview：针对性的复习建议（2-3条）\n" +
                    "4. suggestedNextStep：下一步学习建议（一句话）\n" +
                    "5. 每条内容简短具体，不超过30字\n" +
                    "6. 只返回JSON，不要其他文字",
                    dialogSummary.toString()
            );

            String aiResponse = aiClient.chat("费曼教学质量分析", aiPrompt);
            Map<String, Object> analysis = parseAnalysisJson(aiResponse);

            @SuppressWarnings("unchecked")
            List<String> mp = (List<String>) analysis.get("missingPoints");
            @SuppressWarnings("unchecked")
            List<String> mc = (List<String>) analysis.get("misconceptions");
            @SuppressWarnings("unchecked")
            List<String> rr = (List<String>) analysis.get("recommendedReview");
            String sns = (String) analysis.getOrDefault("suggestedNextStep", "");

            if (mp != null) missingPoints.addAll(mp);
            if (mc != null) misconceptions.addAll(mc);
            if (rr != null) recommendedReview.addAll(rr);
            if (sns != null && !sns.isEmpty()) {
                suggestedNextStep = sns;
            } else {
                suggestedNextStep = buildDefaultNextStep(session.getKnowledgeName(), totalScore);
            }
        } catch (Exception e) {
            log.warn("AI教学总结生成失败，使用规则兜底: {}", e.getMessage());
            // 规则兜底
            buildRuleBasedAnalysis(session.getKnowledgeName(), totalScore,
                    totalAccuracy, totalCompleteness, totalDepth, totalClarity, totalAbilityToExample,
                    roundCount, session.getRounds(), missingPoints, misconceptions, recommendedReview);
            suggestedNextStep = buildDefaultNextStep(session.getKnowledgeName(), totalScore);
        }

        if (missingPoints.isEmpty() && misconceptions.isEmpty()) {
            missingPoints.add("整体表现不错，可以尝试更深入的学习");
        }
        if (recommendedReview.isEmpty()) {
            recommendedReview.add("继续保持，尝试教别人来加深理解");
        }

        session.setFinalAssessment(FinalAssessment.builder()
                .overallScore(totalScore)
                .missingPoints(missingPoints)
                .misconceptions(misconceptions)
                .recommendedReview(recommendedReview)
                .suggestedNextStep(suggestedNextStep)
                .build());
    }

    /** 解析AI返回的分析JSON */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAnalysisJson(String aiResponse) {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (aiResponse == null) throw new RuntimeException("AI返回为空");

        String json = aiResponse.trim()
                .replaceAll("(?s)```json", "").replaceAll("(?s)```", "").trim();

        int startIdx = json.indexOf("{");
        int endIdx = json.lastIndexOf("}");
        if (startIdx >= 0 && endIdx > startIdx) {
            json = json.substring(startIdx, endIdx + 1);
        }

        try {
            return mapper.readValue(json, Map.class);
        } catch (Exception e) {
            // 尝试修复括号
            try {
                int braceCount = 0, bracketCount = 0;
                boolean inStr = false, esc = false;
                for (char c : json.toCharArray()) {
                    if (esc) { esc = false; continue; }
                    if (c == '\\') { esc = true; continue; }
                    if (c == '"') { inStr = !inStr; continue; }
                    if (inStr) continue;
                    if (c == '{') braceCount++;
                    else if (c == '}') braceCount--;
                    else if (c == '[') bracketCount++;
                    else if (c == ']') bracketCount--;
                }
                StringBuilder sb = new StringBuilder(json);
                if (inStr) sb.append('"');
                for (int i = 0; i < bracketCount; i++) sb.append(']');
                for (int i = 0; i < braceCount; i++) sb.append('}');
                return mapper.readValue(sb.toString(), Map.class);
            } catch (Exception e2) {
                throw new RuntimeException("JSON解析失败: " + e2.getMessage());
            }
        }
    }

    /** 规则兜底分析 */
    private void buildRuleBasedAnalysis(String knowledgeName, double totalScore,
                                        double totalAccuracy, double totalCompleteness,
                                        double totalDepth, double totalClarity,
                                        double totalAbilityToExample, int roundCount,
                                        List<Round> rounds,
                                        List<String> missingPoints, List<String> misconceptions,
                                        List<String> recommendedReview) {
        if (totalAccuracy < 0.6) {
            missingPoints.add("对「" + knowledgeName + "」的核心概念理解不够准确");
            recommendedReview.add("重新阅读核心定义和基本原理");
        }
        if (totalCompleteness < 0.6) {
            missingPoints.add("解释不够全面，遗漏了重要知识点");
            recommendedReview.add("梳理知识框架，确保覆盖所有要点");
        }
        if (totalDepth < 0.6) {
            missingPoints.add("解释停留在表面，缺乏深入分析");
            recommendedReview.add("尝试从原理层面理解，多问'为什么'");
        }
        if (totalClarity < 0.6) {
            misconceptions.add("表达不够清晰，可能存在概念混淆");
            recommendedReview.add("用自己的话重新组织语言，避免照搬原文");
        }
        if (totalAbilityToExample < 0.6) {
            misconceptions.add("缺乏具体例子支撑，理解可能不够扎实");
            recommendedReview.add("为每个概念准备至少一个实际案例");
        }
        if (roundCount >= 2) {
            RoundAssessment first = rounds.get(0).getAssessment();
            RoundAssessment last = rounds.get(roundCount - 1).getAssessment();
            double firstAvg = (first.getAccuracy() + first.getCompleteness() + first.getDepth()
                    + first.getClarity() + first.getAbilityToExample()) / 5.0;
            double lastAvg = (last.getAccuracy() + last.getCompleteness() + last.getDepth()
                    + last.getClarity() + last.getAbilityToExample()) / 5.0;
            if (lastAvg > firstAvg + 0.1) {
                recommendedReview.add("你的理解在逐步提升，继续保持这种学习节奏！");
            } else if (lastAvg < firstAvg - 0.1) {
                misconceptions.add("后续轮次的表现有所下降，可能需要重新巩固基础");
                recommendedReview.add("建议回顾之前的内容，确保基础扎实后再深入");
            }
        }
    }

    private String buildDefaultNextStep(String knowledgeName, double totalScore) {
        if (totalScore >= 0.85) {
            return "你已经很好地掌握了「" + knowledgeName + "」，可以尝试学习更高级的相关知识！";
        } else if (totalScore >= 0.6) {
            return "你对「" + knowledgeName + "」有了基本理解，建议针对薄弱点再练习一轮";
        } else {
            return "建议重新学习「" + knowledgeName + "」的基础知识，然后再来挑战";
        }
    }

    private FeynmanRespondResult buildResult(FeynmanSession session, String aiQuestion,
                                              String spiritReaction, boolean complete) {
        int roundNum = session.getRounds().size();
        FeynmanSession.Round lastRound = session.getRounds().isEmpty() ? null
                : session.getRounds().get(roundNum - 1);

        FeynmanRespondResult result = new FeynmanRespondResult()
                .sessionId(session.getSessionId())
                .roundNumber(roundNum)
                .aiQuestion(aiQuestion)
                .spiritReaction(spiritReaction)
                .sessionComplete(complete);

        if (lastRound != null && lastRound.getAssessment() != null) {
            FeynmanSession.RoundAssessment a = lastRound.getAssessment();
            result.accuracy(a.getAccuracy())
                    .completeness(a.getCompleteness())
                    .depth(a.getDepth())
                    .clarity(a.getClarity())
                    .abilityToExample(a.getAbilityToExample())
                    .overallScore((a.getAccuracy() + a.getCompleteness() +
                            a.getDepth() + a.getClarity() + a.getAbilityToExample()) / 5.0);
        }
        return result;
    }
}
