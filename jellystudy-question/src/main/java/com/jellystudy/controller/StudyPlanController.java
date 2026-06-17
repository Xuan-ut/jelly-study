package com.jellystudy.controller;

import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.dubbo.StudyProgressDubboService;
import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.StudyPlan;
import org.apache.dubbo.config.annotation.DubboReference;
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

            if (planId != null && !planId.isEmpty() && stageId != null && !stageId.isEmpty()) {
                try {
                    StudyPlan plan = studyPlanService.findById(planId);
                    if (plan != null && plan.getStages() != null) {
                        for (StudyPlan.PlanStage stage : plan.getStages()) {
                            if (stageId.equals(stage.getStageId())) {
                                int currentProgress = stage.getProgress();
                                int newProgress = Math.min(100, currentProgress + Math.max(1, duration / 2));
                                studyPlanService.updateStageProgress(planId, stageId, newProgress);
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // ignore stage progress update failure
                }
            }

            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @PutMapping("/{planId}/stages/{stageId}/complete")
    public Map<String, Object> completeStage(@PathVariable String planId, @PathVariable String stageId, @RequestBody Map<String, Object> body) {
        Map<String, Object> result = new HashMap<>();
        try {
            int actualDuration = body.containsKey("actualDuration") ? Integer.parseInt(body.get("actualDuration").toString()) : 0;
            StudyPlan plan = studyPlanService.completeStage(planId, stageId, actualDuration);
            result.put("success", true);
            result.put("plan", plan);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{planId}/statistics")
    public Map<String, Object> getPlanStatistics(@PathVariable String planId) {
        return studyPlanService.getPlanStatistics(planId);
    }

    @GetMapping("/{planId}/ai-study-path")
    public Map<String, Object> getAIStudyPath(@PathVariable String planId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String path = studyPlanService.getAIStudyPathRecommendation(userId, planId);
            result.put("success", true);
            result.put("recommendation", path);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{planId}/ai-weak-points")
    public Map<String, Object> getAIWeakPoints(@PathVariable String planId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String analysis = studyPlanService.getAIWeakPointAnalysis(userId, planId);
            result.put("success", true);
            result.put("analysis", analysis);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
    }

    @GetMapping("/{planId}/ai-daily-plan")
    public Map<String, Object> getAIDailyPlan(@PathVariable String planId, @RequestParam Long userId) {
        Map<String, Object> result = new HashMap<>();
        try {
            String dailyPlan = studyPlanService.getAIDailyPlan(userId, planId);
            result.put("success", true);
            result.put("dailyPlan", dailyPlan);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", e.getMessage());
        }
        return result;
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
