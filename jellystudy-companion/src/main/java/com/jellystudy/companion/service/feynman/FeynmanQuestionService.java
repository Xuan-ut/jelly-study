package com.jellystudy.companion.service.feynman;

import com.jellystudy.companion.ai.model.AssessmentResult;

/**
 * 费曼追问策略服务接口
 */
public interface FeynmanQuestionService {

    /** 生成首轮提问 */
    String generateFirstQuestion(String knowledgeName, String knowledgeContent);

    /** 根据用户解释评估结果生成追问 */
    String generateFollowUpQuestion(String knowledgeName, String userExplanation,
                                     AssessmentResult lastAssessment);
}
