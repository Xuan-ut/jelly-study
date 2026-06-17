package com.jellystudy.dubbo;

import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.StudyPlan;

import java.util.List;
import java.util.Map;

public interface StudyPlanDubboService {
    StudyPlan create(StudyPlan studyPlan);
    StudyPlan findById(String id);
    List<StudyPlan> findByUserId(Long userId);
    StudyPlan update(StudyPlan studyPlan);
    void delete(String id);
    StudyPlan updateStageProgress(String planId, String stageId, int progress);
    StudyPlan generatePlan(Long userId, List<String> knowledgePointIds);
    String getAIRecommendation(Long userId, String planId);
    StudyPlan completeStage(String planId, String stageId, int actualDuration);
    Map<String, Object> getPlanStatistics(String planId);
    String getAIStudyPathRecommendation(Long userId, String planId);
    String getAIWeakPointAnalysis(Long userId, String planId);
    String getAIDailyPlan(Long userId, String planId);
    DailyTaskBoard getDailyTaskBoard(Long userId, String date);
    DailyTaskBoard saveDailyTaskBoard(DailyTaskBoard taskBoard);
    List<DailyTaskBoard> getDailyTaskBoardHistory(Long userId, int days);
    StudyPlan updatePlanStages(String planId, List<StudyPlan.PlanStage> stages);
}
