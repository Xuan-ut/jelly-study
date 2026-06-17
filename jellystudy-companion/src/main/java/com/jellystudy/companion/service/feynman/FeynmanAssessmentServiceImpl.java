package com.jellystudy.companion.service.feynman;

import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.model.AssessmentResult;
import com.jellystudy.companion.ai.prompt.FeynmanPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 费曼理解度评估服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeynmanAssessmentServiceImpl implements FeynmanAssessmentService {

    private final AIClient aiClient;
    private final FeynmanPromptBuilder promptBuilder;

    @Override
    public AssessmentResult assess(String knowledgeContent, String userExplanation) {
        String prompt = promptBuilder.buildAssessmentPrompt(knowledgeContent, userExplanation);
        AssessmentResult result = aiClient.chatForAssessment(
                "评估用户对知识点的理解",
                prompt
        );

        // 校验结果
        if (result.getOverallScore() < 0 || result.getOverallScore() > 1.0) {
            log.warn("AI评估结果overallScore异常: {}, 修正为0.5", result.getOverallScore());
            result.setOverallScore(0.5);
        }
        log.info("费曼评估完成: overallScore={}, missingPoints={}", result.getOverallScore(),
                result.getMissingPoints());
        return result;
    }
}
