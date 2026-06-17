package com.jellystudy.companion.service.feynman;

import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.model.AssessmentResult;
import com.jellystudy.companion.ai.prompt.FeynmanPromptBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 费曼追问策略服务
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FeynmanQuestionServiceImpl implements FeynmanQuestionService {

    private final AIClient aiClient;
    private final FeynmanPromptBuilder promptBuilder;

    @Override
    public String generateFirstQuestion(String knowledgeName, String knowledgeContent) {
        String prompt = promptBuilder.buildFirstQuestionPrompt(knowledgeName, knowledgeContent);
        String question = aiClient.chat("费曼教学首轮提问", prompt);
        log.info("费曼首轮提问生成: knowledge={}, question={}", knowledgeName, question);
        return question;
    }

    @Override
    public String generateFollowUpQuestion(String knowledgeName, String userExplanation,
                                            AssessmentResult lastAssessment) {
        String prompt = promptBuilder.buildFollowUpPrompt(knowledgeName, userExplanation, lastAssessment);
        String question = aiClient.chat("费曼教学追问", prompt);
        log.info("费曼追问生成: knowledge={}, question={}", knowledgeName, question);
        return question;
    }
}
