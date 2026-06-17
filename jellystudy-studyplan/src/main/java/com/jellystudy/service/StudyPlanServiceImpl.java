package com.jellystudy.service;

import com.jellystudy.config.StudyPlanConfig;
import com.jellystudy.dubbo.AIDubboService;
import com.jellystudy.dubbo.KnowledgePointDubboService;
import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.StudyPlan;
import com.jellystudy.entity.StudyPlan.PlanStage;
import com.jellystudy.repository.mongo.DailyTaskBoardRepository;
import com.jellystudy.repository.mongo.StudyPlanRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.StudyPlanDubboService")
public class StudyPlanServiceImpl implements StudyPlanDubboService {

    @Autowired
    private StudyPlanRepository studyPlanRepository;

    @Autowired
    private DailyTaskBoardRepository dailyTaskBoardRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private StudyPlanConfig studyPlanConfig;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.KnowledgePointDubboService", timeout = 30000, check = false)
    private KnowledgePointDubboService knowledgePointService;

    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.AIDubboService", timeout = 120000, check = false)
    private AIDubboService aiService;

    private static final String PLAN_CACHE_PREFIX = "studyplan:info:";
    private static final String PLAN_LIST_CACHE_PREFIX = "studyplan:list:";

    @Override
    public StudyPlan create(StudyPlan studyPlan) {
        studyPlan.setCreateTime(new Date());
        studyPlan.setUpdateTime(new Date());
        studyPlan.setStatus("ACTIVE");
        studyPlan.setTotalProgress(0);

        if (studyPlan.getStages() == null || studyPlan.getStages().isEmpty()) {
            studyPlan.setStages(generateStages(studyPlan.getKnowledgePointIds()));
        } else {
            for (PlanStage stage : studyPlan.getStages()) {
                if (stage.getStageId() == null || stage.getStageId().isEmpty()) {
                    stage.setStageId(UUID.randomUUID().toString());
                }
                if (stage.getStatus() == null || stage.getStatus().isEmpty()) {
                    stage.setStatus("NOT_STARTED");
                }
                if (stage.getProgress() == 0) {
                    stage.setProgress(0);
                }
            }
        }

        StudyPlan saved = studyPlanRepository.save(studyPlan);
        cachePlanInfo(saved);
        invalidatePlanListCache(studyPlan.getUserId());

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("planId", saved.getId());
        eventData.put("title", saved.getTitle());
        eventData.put("userId", studyPlan.getUserId());
        rabbitTemplate.convertAndSend("study.event.exchange", "plan.created", eventData);
        log.info("学习计划创建成功: planId={}, userId={}", saved.getId(), studyPlan.getUserId());
        return saved;
    }

    @Override
    public StudyPlan findById(String id) {
        String cacheKey = PLAN_CACHE_PREFIX + id;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof StudyPlan) {
            return (StudyPlan) cached;
        }
        StudyPlan plan = studyPlanRepository.findById(id).orElse(null);
        if (plan != null) {
            cachePlanInfo(plan);
        }
        return plan;
    }

    @Override
    public List<StudyPlan> findByUserId(Long userId) {
        String cacheKey = PLAN_LIST_CACHE_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List) {
            return (List<StudyPlan>) cached;
        }
        List<StudyPlan> plans = studyPlanRepository.findByUserId(userId);
        redisTemplate.opsForValue().set(cacheKey, plans, 10, TimeUnit.MINUTES);
        return plans;
    }

    @Override
    public StudyPlan update(StudyPlan studyPlan) {
        studyPlan.setUpdateTime(new Date());
        StudyPlan saved = studyPlanRepository.save(studyPlan);
        cachePlanInfo(saved);
        invalidatePlanListCache(studyPlan.getUserId());
        return saved;
    }

    @Override
    public void delete(String id) {
        StudyPlan plan = findById(id);
        if (plan != null) {
            studyPlanRepository.deleteById(id);
            redisTemplate.delete(PLAN_CACHE_PREFIX + id);
            invalidatePlanListCache(plan.getUserId());
        }
    }

    @Override
    public StudyPlan updateStageProgress(String planId, String stageId, int progress) {
        StudyPlan plan = findById(planId);
        if (plan == null) return null;

        for (PlanStage stage : plan.getStages()) {
            if (stage.getStageId().equals(stageId)) {
                stage.setProgress(Math.min(100, progress));
                stage.setStatus(progress >= 100 ? "COMPLETED" : "IN_PROGRESS");
                break;
            }
        }

        int totalProgress = calculateTotalProgress(plan);
        plan.setTotalProgress(totalProgress);
        if (totalProgress >= 100) {
            plan.setStatus("COMPLETED");
        }

        StudyPlan saved = update(plan);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("planId", planId);
        eventData.put("stageId", stageId);
        eventData.put("progress", progress);
        eventData.put("userId", plan.getUserId());
        String routingKey = progress >= 100 ? "stage.completed" : "progress.updated";
        rabbitTemplate.convertAndSend("study.event.exchange", routingKey, eventData);

        return saved;
    }

    @Override
    public StudyPlan generatePlan(Long userId, List<String> knowledgePointIds) {
        StudyPlan plan = new StudyPlan();
        plan.setUserId(userId);
        plan.setKnowledgePointIds(knowledgePointIds);

        List<String> kpNames = new ArrayList<>();
        for (String kpId : knowledgePointIds) {
            try {
                KnowledgePoint kp = knowledgePointService.findById(kpId);
                if (kp != null) {
                    kpNames.add(kp.getName());
                }
            } catch (Exception e) {
                log.warn("获取知识点失败: {}", kpId);
            }
        }
        plan.setKnowledgePointNames(kpNames);
        plan.setTitle("学习计划: " + String.join(", ", kpNames));
        plan.setDescription("基于知识点 " + String.join("、", kpNames) + " 自动生成的学习计划");

        return create(plan);
    }

    @Override
    public String getAIRecommendation(Long userId, String planId) {
        StudyPlan plan = findById(planId);
        if (plan == null) return "计划不存在";

        StringBuilder sb = new StringBuilder();
        sb.append("用户当前学习计划: ").append(plan.getTitle()).append("\n");
        sb.append("总进度: ").append(plan.getTotalProgress()).append("%\n");
        sb.append("各阶段进度:\n");
        for (PlanStage stage : plan.getStages()) {
            sb.append("- ").append(stage.getName()).append(": ").append(stage.getProgress()).append("% (").append(stage.getStatus()).append(")\n");
        }
        sb.append("\n请根据以上学习进度，给出下一步学习建议和推荐。");

        try {
            return aiService.chat(sb.toString());
        } catch (Exception e) {
            log.warn("AI推荐失败: {}", e.getMessage());
            return "AI推荐暂时不可用，请继续按计划学习。当前进度: " + plan.getTotalProgress() + "%";
        }
    }

    @Override
    public StudyPlan completeStage(String planId, String stageId, int actualDuration) {
        StudyPlan plan = findById(planId);
        if (plan == null) return null;

        for (PlanStage stage : plan.getStages()) {
            if (stage.getStageId().equals(stageId)) {
                stage.setProgress(100);
                stage.setStatus("COMPLETED");
                break;
            }
        }

        int totalProgress = calculateTotalProgress(plan);
        plan.setTotalProgress(totalProgress);
        if (totalProgress >= 100) {
            plan.setStatus("COMPLETED");
        }

        StudyPlan saved = update(plan);

        Map<String, Object> eventData = new HashMap<>();
        eventData.put("planId", planId);
        eventData.put("stageId", stageId);
        eventData.put("actualDuration", actualDuration);
        eventData.put("userId", plan.getUserId());
        rabbitTemplate.convertAndSend("study.event.exchange", "stage.completed", eventData);

        return saved;
    }

    @Override
    public Map<String, Object> getPlanStatistics(String planId) {
        StudyPlan plan = findById(planId);
        Map<String, Object> stats = new HashMap<>();
        if (plan == null) return stats;

        stats.put("totalStages", plan.getStages() != null ? plan.getStages().size() : 0);
        stats.put("completedStages", 0);
        stats.put("inProgressStages", 0);
        stats.put("notStartedStages", 0);
        stats.put("overallProgress", plan.getTotalProgress());

        if (plan.getStages() != null) {
            for (PlanStage stage : plan.getStages()) {
                String status = stage.getStatus();
                if ("COMPLETED".equals(status)) {
                    long count = (long) stats.get("completedStages");
                    stats.put("completedStages", count + 1);
                } else if ("IN_PROGRESS".equals(status)) {
                    long count = (long) stats.get("inProgressStages");
                    stats.put("inProgressStages", count + 1);
                } else {
                    long count = (long) stats.get("notStartedStages");
                    stats.put("notStartedStages", count + 1);
                }
            }
        }

        stats.put("status", plan.getStatus());
        stats.put("createTime", plan.getCreateTime());
        return stats;
    }

    @Override
    public String getAIStudyPathRecommendation(Long userId, String planId) {
        StudyPlan plan = findById(planId);
        if (plan == null) return "计划不存在";

        List<String> kpNames = plan.getKnowledgePointNames() != null ? plan.getKnowledgePointNames() : new ArrayList<>();
        try {
            return aiService.generateStudyPath(plan.getTitle(), "当前进度" + plan.getTotalProgress() + "%", "全面掌握", kpNames);
        } catch (Exception e) {
            log.warn("AI学习路径推荐失败: {}", e.getMessage());
            return "AI学习路径推荐暂时不可用。";
        }
    }

    @Override
    public String getAIWeakPointAnalysis(Long userId, String planId) {
        StudyPlan plan = findById(planId);
        if (plan == null) return "计划不存在";

        List<Map<String, Object>> studyRecords = new ArrayList<>();
        if (plan.getStages() != null) {
            for (PlanStage stage : plan.getStages()) {
                Map<String, Object> record = new HashMap<>();
                record.put("stageName", stage.getName());
                record.put("progress", stage.getProgress());
                record.put("status", stage.getStatus());
                studyRecords.add(record);
            }
        }
        try {
            return aiService.analyzeWeakPoints(userId, studyRecords);
        } catch (Exception e) {
            log.warn("AI薄弱点分析失败: {}", e.getMessage());
            return "AI薄弱点分析暂时不可用。";
        }
    }

    @Override
    public String getAIDailyPlan(Long userId, String planId) {
        StudyPlan plan = findById(planId);
        if (plan == null) return "计划不存在";

        List<Map<String, Object>> recentProgress = new ArrayList<>();
        if (plan.getStages() != null) {
            for (PlanStage stage : plan.getStages()) {
                Map<String, Object> progress = new HashMap<>();
                progress.put("stageName", stage.getName());
                progress.put("progress", stage.getProgress());
                progress.put("status", stage.getStatus());
                recentProgress.add(progress);
            }
        }
        try {
            return aiService.generateDailyPlan(userId, planId, recentProgress);
        } catch (Exception e) {
            log.warn("AI每日计划生成失败: {}", e.getMessage());
            return "AI每日计划生成暂时不可用。";
        }
    }

    private List<PlanStage> generateStages(List<String> knowledgePointIds) {
        List<PlanStage> stages = new ArrayList<>();
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) return stages;

        int batchSize = studyPlanConfig.getKnowledgePointsPerStage();
        int order = 1;
        for (int i = 0; i < knowledgePointIds.size(); i += batchSize) {
            List<String> batch = knowledgePointIds.subList(i, Math.min(i + batchSize, knowledgePointIds.size()));
            PlanStage stage = new PlanStage();
            stage.setStageId(UUID.randomUUID().toString());
            stage.setName("阶段 " + order);
            stage.setDescription("学习阶段 " + order + " 的知识点");
            stage.setKnowledgePointIds(new ArrayList<>(batch));
            stage.setProgress(0);
            stage.setStatus("NOT_STARTED");
            stage.setOrder(order);
            stages.add(stage);
            order++;
        }
        return stages;
    }

    private int calculateTotalProgress(StudyPlan plan) {
        if (plan.getStages() == null || plan.getStages().isEmpty()) return 0;
        int total = 0;
        for (PlanStage stage : plan.getStages()) {
            total += stage.getProgress();
        }
        return total / plan.getStages().size();
    }

    private void cachePlanInfo(StudyPlan plan) {
        redisTemplate.opsForValue().set(PLAN_CACHE_PREFIX + plan.getId(), plan, 30, TimeUnit.MINUTES);
    }

    private void invalidatePlanListCache(Long userId) {
        redisTemplate.delete(PLAN_LIST_CACHE_PREFIX + userId);
    }

    @Override
    public DailyTaskBoard getDailyTaskBoard(Long userId, String date) {
        return dailyTaskBoardRepository.findByUserIdAndDate(userId, date).orElse(null);
    }

    @Override
    public DailyTaskBoard saveDailyTaskBoard(DailyTaskBoard taskBoard) {
        if (taskBoard.getId() == null || taskBoard.getId().isEmpty()) {
            DailyTaskBoard existing = dailyTaskBoardRepository
                .findByUserIdAndDate(taskBoard.getUserId(), taskBoard.getDate())
                .orElse(null);
            if (existing != null) {
                taskBoard.setId(existing.getId());
                taskBoard.setCreateTime(existing.getCreateTime());
            }
        }
        taskBoard.setUpdateTime(new Date());
        if (taskBoard.getCreateTime() == null) {
            taskBoard.setCreateTime(new Date());
        }
        if (taskBoard.getTasks() != null) {
            for (DailyTaskBoard.DailyTask task : taskBoard.getTasks()) {
                if (task.getTaskId() == null || task.getTaskId().isEmpty()) {
                    task.setTaskId(UUID.randomUUID().toString());
                }
            }
        }
        return dailyTaskBoardRepository.save(taskBoard);
    }

    @Override
    public List<DailyTaskBoard> getDailyTaskBoardHistory(Long userId, int days) {
        java.time.LocalDate today = java.time.LocalDate.now();
        String endDate = today.toString();
        String startDate = today.minusDays(days - 1).toString();
        return dailyTaskBoardRepository.findByUserIdAndDateBetween(userId, startDate, endDate);
    }

    @Override
    public StudyPlan updatePlanStages(String planId, List<PlanStage> stages) {
        StudyPlan plan = findById(planId);
        if (plan == null) return null;

        for (PlanStage stage : stages) {
            if (stage.getStageId() == null || stage.getStageId().isEmpty()) {
                stage.setStageId(UUID.randomUUID().toString());
            }
            if (stage.getStatus() == null || stage.getStatus().isEmpty()) {
                stage.setStatus("NOT_STARTED");
            }
        }

        plan.setStages(stages);
        int totalProgress = calculateTotalProgress(plan);
        plan.setTotalProgress(totalProgress);
        if (totalProgress >= 100) {
            plan.setStatus("COMPLETED");
        } else if ("COMPLETED".equals(plan.getStatus())) {
            plan.setStatus("ACTIVE");
        }

        StudyPlan saved = update(plan);
        log.info("学习计划阶段更新成功: planId={}, stageCount={}", planId, stages.size());
        return saved;
    }
}
