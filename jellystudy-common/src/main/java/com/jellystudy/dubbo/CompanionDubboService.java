package com.jellystudy.dubbo;

import com.jellystudy.entity.*;

import java.util.List;

/**
 * AI学习伴侣 Dubbo 服务接口
 * 完整对应设计文档第七章的接口设计
 */
public interface CompanionDubboService {

    // ===== 精灵系统 =====

    /** 获取精灵状态 */
    SpiritStateDTO getSpiritState(Long userId);

    /** 获取精灵问候语（根据情感状态动态生成） */
    SpiritGreetingDTO getSpiritGreeting(Long userId);

    /** 精灵对话 */
    SpiritChatResponseDTO chatWithSpirit(Long userId, String sessionId, String message);

    /** 喂养精灵（内部调用，由事件触发） */
    SpiritStateDTO feedSpirit(Long userId, String eventType, int feedValue);

    // ===== 费曼反转教学 =====

    /** 开始费曼教学会话 */
    FeynmanSessionDTO startFeynmanSession(Long userId, String knowledgeId);

    /** 费曼教学回应（用户解释后AI追问） */
    FeynmanResponseDTO feynmanRespond(String sessionId, String userExplanation);

    /** 获取理解度评估 */
    UnderstandingAssessmentDTO getUnderstandingAssessment(String sessionId);

    // ===== 时空预测 =====

    /** 获取知识树当前快照 */
    KnowledgeTreeSnapshotDTO getKnowledgeTree(Long userId);

    /** 获取知识树未来预测 */
    KnowledgeTreePredictionDTO predictKnowledgeTree(Long userId, int daysAhead);

    /** 获取学习轨迹回溯 */
    LearningTimelineDTO getLearningTimeline(Long userId);

    /** 获取关键节点预警 */
    List<EarlyWarningDTO> getEarlyWarnings(Long userId);

    // ===== 群体智慧蜂巢 =====

    /** 获取群体学习模式 */
    List<HivePatternDTO> getHivePatterns(String subject);

    /** 获取个体异常检测报告 */
    AnomalyReportDTO getAnomalyReport(Long userId);

    /** 获取推荐学习路径 */
    List<LearningPathRecommendationDTO> getRecommendedPaths(Long userId, String subject);

    /** 获取学习健康报告 */
    LearningHealthReportDTO getHealthReport(Long userId);
}
