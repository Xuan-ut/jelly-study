package com.jellystudy.companion.service.feynman;

import com.jellystudy.companion.ai.model.AssessmentResult;

/**
 * 费曼理解度评估服务接口
 */
public interface FeynmanAssessmentService {

    /** 评估用户对知识点的解释质量 */
    AssessmentResult assess(String knowledgeContent, String userExplanation);
}
