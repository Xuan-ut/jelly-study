package com.jellystudy.companion.service.feynman;

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

    private void concludeSession(FeynmanSession session) {
        session.setStatus("COMPLETED");
        // 计算总评
        double totalScore = 0;
        if (!session.getRounds().isEmpty()) {
            for (Round r : session.getRounds()) {
                totalScore += (r.getAssessment().getAccuracy() +
                        r.getAssessment().getCompleteness() +
                        r.getAssessment().getDepth() +
                        r.getAssessment().getClarity() +
                        r.getAssessment().getAbilityToExample()) / 5.0;
            }
            totalScore /= session.getRounds().size();
        }
        session.setFinalAssessment(FinalAssessment.builder()
                .overallScore(totalScore)
                .missingPoints(new ArrayList<>())
                .misconceptions(new ArrayList<>())
                .recommendedReview(new ArrayList<>())
                .suggestedNextStep(totalScore >= 0.85 ? "你已经掌握了这个知识点！" : "建议回顾相关知识后再试一次")
                .build());
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
