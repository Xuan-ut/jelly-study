package com.jellystudy.dubbo;

import com.jellystudy.entity.StudyProgress;

import java.util.List;
import java.util.Map;

public interface StudyProgressDubboService {
    StudyProgress create(StudyProgress progress);
    StudyProgress update(StudyProgress progress);
    StudyProgress findByUserAndKnowledgePoint(Long userId, String knowledgePointId);
    List<StudyProgress> findByUserId(Long userId);
    List<StudyProgress> findByPlanId(String planId);
    Map<String, Object> getUserStudyStats(Long userId);
    void recordStudyEvent(Long userId, String planId, String stageId, String knowledgePointId, int duration);
}
