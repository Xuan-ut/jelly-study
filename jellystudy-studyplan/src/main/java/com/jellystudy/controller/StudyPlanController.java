package com.jellystudy.controller;

import com.jellystudy.config.StudyPlanConfig;
import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.dubbo.StudyProgressDubboService;
import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.StudyPlan;
import com.jellystudy.entity.StudyProgress;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/study-plans")
public class StudyPlanController {

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.StudyPlanDubboService", timeout = 120000, check = false)
    private StudyPlanDubboService studyPlanService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.StudyProgressDubboService", timeout = 30000, check = false)
    private StudyProgressDubboService progressService;

    @Autowired
    private StudyPlanConfig studyPlanConfig;

    @PostMapping
    public Map<String, Object> createPlan(@RequestBody StudyPlan studyPlan) {
        Map<String, Object> result = new HashMap<>();
        try {
            StudyPlan created = studyPlanService.create(studyPlan);
            result.put("success", true);
            result.put("plan", created);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/generate")
    public Map<String, Object> generatePlan(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            @SuppressWarnings("unchecked")
            List<String> kpIds = (List<String>) body.get("knowledgePointIds");
            StudyPlan plan = studyPlanService.generatePlan(userId, kpIds);
            result.put("success", true);
            result.put("plan", plan);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{id}")
    public Map<String, Object> getPlan(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        StudyPlan plan = studyPlanService.findById(id);
        if (plan != null) {
            result.put("success", true);
            result.put("plan", plan);
        } else {
            result.put("success", false);
            result.put("message", "计划不存在");
        }
        return result;
    }

    @GetMapping("/user/{userId}")
    public List<StudyPlan> getUserPlans(@PathVariable Long userId) {
        return studyPlanService.findByUserId(userId);
    }

    @PutMapping("/{planId}/stages/{stageId}/progress")
    public Map<String, Object> updateStageProgress(@PathVariable String planId, @PathVariable String stageId, @RequestBody Map<String, Integer> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            StudyPlan plan = studyPlanService.updateStageProgress(planId, stageId, body.get("progress"));
            result.put("success", true);
            result.put("plan", plan);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> deletePlan(@PathVariable String id) {
        Map<String, Object> result = new HashMap<>();
        try {
            studyPlanService.delete(id);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{planId}/ai-recommendation")
    public Map<String, Object> getAIRecommendation(@PathVariable String planId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String recommendation = studyPlanService.getAIRecommendation(userId, planId);
            result.put("success", true);
            result.put("recommendation", recommendation);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/progress/user/{userId}")
    public Map<String, Object> getUserStudyStats(@PathVariable Long userId) {
        return progressService.getUserStudyStats(userId);
    }

    @PostMapping("/progress/record")
    public Map<String, Object> recordStudy(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            Long userId = Long.valueOf(body.get("userId").toString());
            String planId = (String) body.get("planId");
            String stageId = (String) body.get("stageId");
            String kpId = (String) body.get("knowledgePointId");
            int duration = body.containsKey("duration") ? Integer.parseInt(body.get("duration").toString()) : 30;
            progressService.recordStudyEvent(userId, planId, stageId, kpId, duration);
            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/nacos-config")
    public Map<String, Object> getNacosConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("source", "Nacos Config Center");
        config.put("knowledgePointsPerStage", studyPlanConfig.getKnowledgePointsPerStage());
        config.put("defaultStudyDuration", studyPlanConfig.getDefaultStudyDuration());
        config.put("maxActivePlans", studyPlanConfig.getMaxActivePlans());
        config.put("achievementEnabled", studyPlanConfig.isAchievementEnabled());
        config.put("welcomeMessage", studyPlanConfig.getWelcomeMessage());
        return config;
    }

    @PutMapping("/{planId}/stages")
    public Map<String, Object> updatePlanStages(@PathVariable String planId, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stageMaps = (List<Map<String, Object>>) body.get("stages");
            List<StudyPlan.PlanStage> stages = new ArrayList<>();
            if (stageMaps != null) {
                for (Map<String, Object> sm : stageMaps) {
                    StudyPlan.PlanStage stage = new StudyPlan.PlanStage();
                    stage.setStageId(sm.get("stageId") != null ? sm.get("stageId").toString() : null);
                    stage.setName(sm.get("name") != null ? sm.get("name").toString() : "");
                    stage.setDescription(sm.get("description") != null ? sm.get("description").toString() : "");
                    stage.setProgress(sm.get("progress") != null ? Integer.parseInt(sm.get("progress").toString()) : 0);
                    stage.setStatus(sm.get("status") != null ? sm.get("status").toString() : "NOT_STARTED");
                    stage.setOrder(sm.get("order") != null ? Integer.parseInt(sm.get("order").toString()) : 0);
                    if (sm.get("estimatedHours") != null) {
                        stage.setEstimatedHours(Double.parseDouble(sm.get("estimatedHours").toString()));
                    }
                    if (sm.get("tasks") instanceof List) {
                        List<String> tasks = new ArrayList<>();
                        for (Object t : (List<?>) sm.get("tasks")) {
                            tasks.add(t.toString());
                        }
                        stage.setTasks(tasks);
                    }
                    stages.add(stage);
                }
            }
            StudyPlan plan = studyPlanService.updatePlanStages(planId, stages);
            if (plan != null) {
                result.put("success", true);
                result.put("plan", plan);
            } else {
                result.put("success", false);
                result.put("message", "计划不存在");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/daily-tasks")
    public Map<String, Object> getDailyTaskBoard(@RequestParam Long userId, @RequestParam String date) {
        Map<String, Object> result = new HashMap<>();
        try {
            DailyTaskBoard board = studyPlanService.getDailyTaskBoard(userId, date);
            result.put("success", true);
            result.put("taskBoard", board);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PostMapping("/daily-tasks")
    public Map<String, Object> saveDailyTaskBoard(@RequestBody DailyTaskBoard taskBoard) {
        Map<String, Object> result = new HashMap<>();
        try {
            DailyTaskBoard saved = studyPlanService.saveDailyTaskBoard(taskBoard);
            result.put("success", true);
            result.put("taskBoard", saved);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/daily-tasks/history")
    public Map<String, Object> getDailyTaskBoardHistory(@RequestParam Long userId, @RequestParam(defaultValue = "7") int days) {
        Map<String, Object> result = new HashMap<>();
        try {
            List<DailyTaskBoard> history = studyPlanService.getDailyTaskBoardHistory(userId, days);
            result.put("success", true);
            result.put("history", history);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }
}
