package com.jellystudy.dubbo;

import java.util.List;
import java.util.Map;

public interface AIDubboService {
    String chat(String message);
    
    String analyzeComments(List<String> comments);
    
    String analyzeQuestion(String questionContent);
    
    String summarizeText(String text);

    String generateStudyPath(String subject, String currentLevel, String goal, List<String> knowledgePoints);

    String analyzeWeakPoints(Long userId, List<Map<String, Object>> studyRecords);

    String generateDailyPlan(Long userId, String planId, List<Map<String, Object>> recentProgress);

    String analyzeUserBehavior(Long userId, List<Map<String, Object>> activities);

    String generatePlanDetail(String subject, String goal, int stageCount, String difficulty);
}