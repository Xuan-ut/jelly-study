package com.jellystudy.companion.service.timeline;

import com.jellystudy.companion.ai.AIClient;
import com.jellystudy.companion.ai.prompt.TimelinePromptBuilder;
import com.jellystudy.companion.config.TimelineConfigProperties;
import com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Branch;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Node;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Prediction;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Scenario;
import com.jellystudy.companion.repository.KnowledgeTreeSnapshotRepository;
import com.jellystudy.companion.util.ForgettingCurveUtil;
import com.jellystudy.entity.KnowledgePoint;
import com.jellystudy.entity.StudyPlan;
import com.jellystudy.entity.StudyProgress;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeTreeServiceImpl implements KnowledgeTreeService {

    private final KnowledgeTreeSnapshotRepository snapshotRepository;
    private final StudyPlanServiceConsumer studyPlanServiceConsumer;
    private final AIClient aiClient;
    private final TimelinePromptBuilder timelinePromptBuilder;
    private final TimelineConfigProperties timelineConfig;

    @Override
    public KnowledgeTreeSnapshot buildTree(Long userId) {
        log.info("构建用户知识树（计划维度）: userId={}", userId);

        // 从学习计划服务获取真实数据
        List<StudyPlan> plans = studyPlanServiceConsumer.getUserPlans(userId);
        List<StudyProgress> progressList = studyPlanServiceConsumer.getUserProgressRecords(userId);

        // 构建知识点名称 → 掌握度 + 最后复习时间的映射
        Map<String, Double> kpMasteryMap = new HashMap<>();
        Map<String, LocalDate> kpLastReviewMap = new HashMap<>();
        // 构建 stageId → 进度 的映射（用于匹配没有knowledgePointIds的阶段）
        Map<String, Integer> stageProgressMap = new HashMap<>();
        Map<String, LocalDate> stageLastStudyMap = new HashMap<>();

        for (StudyProgress sp : progressList) {
            String kpName = sp.getKnowledgePointName();
            String stageId = sp.getStageId();
            double mastery = sp.getProgress() / 100.0;
            Date createTime = sp.getCreateTime();
            LocalDate ld = null;
            if (createTime != null) {
                ld = createTime instanceof java.sql.Date
                        ? ((java.sql.Date) createTime).toLocalDate()
                        : createTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }

            // 按 knowledgePointName 匹配
            if (kpName != null && !kpName.isEmpty()) {
                kpMasteryMap.merge(kpName, mastery, Math::max);
                if (ld != null) {
                    kpLastReviewMap.merge(kpName, ld, (old, cur) -> cur.isAfter(old) ? cur : old);
                }
            }

            // 按 stageId 匹配（用于没有KP的阶段）
            if (stageId != null && !stageId.isEmpty()) {
                stageProgressMap.merge(stageId, sp.getProgress(), Math::max);
                if (ld != null) {
                    stageLastStudyMap.merge(stageId, ld, (old, cur) -> cur.isAfter(old) ? cur : old);
                }
            }
        }

        // 如果没有任何计划，创建空树
        if (plans == null || plans.isEmpty()) {
            log.info("用户无学习计划，返回空知识树");
            return createEmptyTree(userId);
        }

        // 按学习计划构建知识树：每个计划 = 一个 Branch
        List<Branch> branches = new ArrayList<>();

        for (StudyPlan plan : plans) {
            String planTitle = plan.getTitle() != null ? plan.getTitle() : "未命名计划";
            String planStatus = plan.getStatus() != null ? plan.getStatus() : "UNKNOWN";

            List<Node> stageNodes = new ArrayList<>();

            // 计划下的阶段作为子节点
            if (plan.getStages() != null) {
                for (StudyPlan.PlanStage stage : plan.getStages()) {
                    String stageName = stage.getName() != null ? stage.getName() : "未命名阶段";
                    String stageId = stage.getStageId() != null ? stage.getStageId() : UUID.randomUUID().toString().substring(0, 8);
                    String stageStatus = stage.getStatus() != null ? stage.getStatus() : "PENDING";

                    List<Node> kpNodes = new ArrayList<>();

                    // 阶段下的知识点作为叶子节点
                    if (stage.getKnowledgePointIds() != null) {
                        for (String kpName : stage.getKnowledgePointIds()) {
                            if (kpName == null || kpName.isEmpty()) continue;
                            double mastery = kpMasteryMap.getOrDefault(kpName, 0.0);
                            LocalDate lastReview = kpLastReviewMap.get(kpName);

                            // 应用遗忘曲线：计算当前有效掌握度
                            double effectiveMastery = applyForgettingCurve(mastery, lastReview);

                            kpNodes.add(Node.builder()
                                    .name(kpName)
                                    .mastery(effectiveMastery)
                                    .lastReview(lastReview)
                                    .type("KNOWLEDGE_POINT")
                                    .nodeId(kpName)
                                    .status(mastery >= 0.8 ? "MASTERED" : mastery > 0 ? "LEARNING" : "PENDING")
                                    .children(new ArrayList<>())
                                    .build());
                        }
                    }

                    // 如果阶段没有直接关联知识点，使用计划级别的知识点
                    if (kpNodes.isEmpty() && plan.getKnowledgePointNames() != null) {
                        for (String kpName : plan.getKnowledgePointNames()) {
                            if (kpName == null || kpName.isEmpty()) continue;
                            double mastery = kpMasteryMap.getOrDefault(kpName, 0.0);
                            LocalDate lastReview = kpLastReviewMap.get(kpName);
                            double effectiveMastery = applyForgettingCurve(mastery, lastReview);

                            kpNodes.add(Node.builder()
                                    .name(kpName)
                                    .mastery(effectiveMastery)
                                    .lastReview(lastReview)
                                    .type("KNOWLEDGE_POINT")
                                    .nodeId(kpName)
                                    .status(mastery >= 0.8 ? "MASTERED" : mastery > 0 ? "LEARNING" : "PENDING")
                                    .children(new ArrayList<>())
                                    .build());
                        }
                    }

                    // 计算阶段掌握度（多数据源融合）：
                    // 优先级: KP平均掌握度 > stageId匹配的进度记录 > 计划中的stage.progress
                    double stageMastery;
                    LocalDate stageReview;

                    double kpAvg = kpNodes.isEmpty() ? 0
                            : kpNodes.stream().mapToDouble(Node::getMastery).average().orElse(0);
                    double stageRecordProgress = stageProgressMap.containsKey(stageId)
                            ? stageProgressMap.get(stageId) / 100.0 : 0;
                    double planStageProgress = stage.getProgress() / 100.0;

                    // 取三者最大值，确保任何来源的进度都能被反映
                    stageMastery = Math.max(kpAvg, Math.max(stageRecordProgress, planStageProgress));

                    // lastReview：优先 KP，其次进度记录
                    if (!kpNodes.isEmpty()) {
                        stageReview = kpNodes.stream()
                                .map(Node::getLastReview)
                                .filter(Objects::nonNull)
                                .max(LocalDate::compareTo)
                                .orElse(null);
                    } else {
                        stageReview = stageLastStudyMap.get(stageId);
                    }
                    // 根据实际掌握度判定状态，而非依赖计划中可能过期的状态
                    String derivedStatus = stageMastery >= 0.8 ? "MASTERED"
                            : stageMastery > 0 ? "IN_PROGRESS" : stageStatus;

                    stageNodes.add(Node.builder()
                            .name(stageName)
                            .mastery(stageMastery)
                            .lastReview(stageReview)
                            .type("STAGE")
                            .nodeId(stageId)
                            .status(derivedStatus)
                            .children(kpNodes)
                            .build());
                }
            }

            // 如果计划没有阶段，将知识点直接挂在计划下
            if (stageNodes.isEmpty() && plan.getKnowledgePointNames() != null) {
                List<Node> kpNodes = new ArrayList<>();
                for (String kpName : plan.getKnowledgePointNames()) {
                    if (kpName == null || kpName.isEmpty()) continue;
                    double mastery = kpMasteryMap.getOrDefault(kpName, 0.0);
                    LocalDate lastReview = kpLastReviewMap.get(kpName);
                    double effectiveMastery = applyForgettingCurve(mastery, lastReview);

                    kpNodes.add(Node.builder()
                            .name(kpName)
                            .mastery(effectiveMastery)
                            .lastReview(lastReview)
                            .type("KNOWLEDGE_POINT")
                            .nodeId(kpName)
                            .status(mastery >= 0.8 ? "MASTERED" : mastery > 0 ? "LEARNING" : "PENDING")
                            .children(new ArrayList<>())
                            .build());
                }
                // 创建一个虚拟阶段来承载知识点
                double stageMastery = kpNodes.stream().mapToDouble(Node::getMastery).average().orElse(0);
                stageNodes.add(Node.builder()
                        .name("知识点列表")
                        .mastery(stageMastery)
                        .lastReview(kpNodes.stream()
                                .map(Node::getLastReview)
                                .filter(Objects::nonNull)
                                .max(LocalDate::compareTo)
                                .orElse(null))
                        .type("STAGE")
                        .nodeId(plan.getId() + "-default")
                        .status("IN_PROGRESS")
                        .children(kpNodes)
                        .build());
            }

            double planProgress = stageNodes.isEmpty() ? plan.getTotalProgress() / 100.0
                    : stageNodes.stream().mapToDouble(Node::getMastery).average().orElse(0);
            double stability = ForgettingCurveUtil.calculateStability(planProgress);
            double retention30d = ForgettingCurveUtil.predictRetention(planProgress, 0, 30);

            // 计算该计划下所有知识点的总遗忘风险
            long staleCount = stageNodes.stream()
                    .flatMap(s -> s.getChildren().stream())
                    .filter(n -> n.getMastery() < 0.3)
                    .count();
            long totalKps = stageNodes.stream()
                    .mapToLong(s -> s.getChildren().size())
                    .sum();

            branches.add(Branch.builder()
                    .name(planTitle)
                    .planId(plan.getId())
                    .planStatus(planStatus)
                    .description(plan.getDescription())
                    .progress(planProgress)
                    .stability(stability)
                    .predictedRetention30d(retention30d)
                    .nodes(stageNodes)
                    .build());

            log.info("计划 [{}]: {} 个阶段, {} 个知识点, 进度={:.0f}%, 遗忘风险={}/{}",
                    planTitle, stageNodes.size(), totalKps, planProgress * 100, staleCount, totalKps);
        }

        KnowledgeTreeSnapshot snapshot = KnowledgeTreeSnapshot.builder()
                .userId(userId).date(LocalDate.now()).branches(branches)
                .prediction(Prediction.builder()
                        .optimistic(Scenario.builder().date(LocalDate.now().plusDays(90))
                                .totalBranches(branches.size() + 2).avgMastery(0.85)
                                .description("保持当前学习节奏，90天后将掌握更多领域").build())
                        .pessimistic(Scenario.builder().date(LocalDate.now().plusDays(90))
                                .totalBranches(branches.size()).avgMastery(0.25)
                                .description("如果停止学习，90天后大部分知识将被遗忘").build())
                        .build())
                .build();

        return snapshotRepository.save(snapshot);
    }

    /**
     * 根据遗忘曲线调整掌握度
     * 如果距上次复习超过7天，掌握度开始衰减
     */
    private double applyForgettingCurve(double rawMastery, LocalDate lastReview) {
        if (rawMastery <= 0) return 0;
        if (lastReview == null) return rawMastery * 0.5; // 从未复习，掌握度打折扣

        long daysSinceReview = java.time.temporal.ChronoUnit.DAYS.between(lastReview, LocalDate.now());
        if (daysSinceReview <= 7) return rawMastery; // 7天内，保持原样

        // 超过7天后应用遗忘曲线
        double stability = ForgettingCurveUtil.calculateStability(rawMastery);
        double retention = ForgettingCurveUtil.calculateRetention((int) daysSinceReview, stability);
        return rawMastery * retention;
    }

    private KnowledgeTreeSnapshot createEmptyTree(Long userId) {
        KnowledgeTreeSnapshot empty = KnowledgeTreeSnapshot.builder()
                .userId(userId).date(LocalDate.now()).branches(new ArrayList<>())
                .prediction(Prediction.builder()
                        .optimistic(Scenario.builder().date(LocalDate.now().plusDays(90))
                                .totalBranches(0).avgMastery(0).description("创建学习计划，种下你的第一棵知识树").build())
                        .pessimistic(Scenario.builder().date(LocalDate.now().plusDays(90))
                                .totalBranches(0).avgMastery(0).description("不开始学习，知识树永远不会生长").build())
                        .build())
                .build();
        return snapshotRepository.save(empty);
    }

    @Override
    public KnowledgeTreeSnapshot getLatestSnapshot(Long userId) {
        return snapshotRepository.findTopByUserIdOrderByDateDesc(userId).orElse(null);
    }

    @Override
    public TimelineService.KnowledgeTreePrediction getTreePrediction(Long userId, int daysAhead) {
        KnowledgeTreeSnapshot current = getLatestSnapshot(userId);
        if (current == null) {
            current = buildTree(userId);
        }

        KnowledgeTreeSnapshot optimistic = buildOptimisticPrediction(current, daysAhead);
        KnowledgeTreeSnapshot pessimistic = buildPessimisticPrediction(current, daysAhead);

        // AI生成描述
        try {
            String optDesc = aiClient.chat("时空预测-乐观",
                    timelinePromptBuilder.buildPredictionPrompt(current, true, daysAhead));
            String pesDesc = aiClient.chat("时空预测-悲观",
                    timelinePromptBuilder.buildPredictionPrompt(current, false, daysAhead));

            if (optimistic.getPrediction() != null && optimistic.getPrediction().getOptimistic() != null) {
                optimistic.getPrediction().getOptimistic().setDescription(optDesc);
            }
            if (pessimistic.getPrediction() != null && pessimistic.getPrediction().getPessimistic() != null) {
                pessimistic.getPrediction().getPessimistic().setDescription(pesDesc);
            }
        } catch (Exception e) {
            log.warn("AI预测描述生成失败: {}", e.getMessage());
        }

        return new TimelineService.KnowledgeTreePrediction()
                .current(current).optimistic(optimistic).pessimistic(pessimistic);
    }

    private KnowledgeTreeSnapshot buildOptimisticPrediction(KnowledgeTreeSnapshot current, int daysAhead) {
        List<Branch> predBranches = new ArrayList<>();
        for (Branch b : current.getBranches()) {
            double growthRate = 0.15 * (daysAhead / 30.0);
            double newProgress = Math.min(1.0, b.getProgress() + growthRate);
            predBranches.add(Branch.builder()
                    .name(b.getName())
                    .progress(newProgress)
                    .stability(Math.min(1.0, b.getStability() + 0.05))
                    .predictedRetention30d(ForgettingCurveUtil.predictRetention(newProgress, 0, 30))
                    .nodes(b.getNodes())
                    .build());
        }
        // 乐观场景：可能解锁新分支
        int newBranches = Math.min(3, daysAhead / 30);
        double avgMastery = predBranches.stream().mapToDouble(Branch::getProgress).average().orElse(0.5);

        KnowledgeTreeSnapshot pred = new KnowledgeTreeSnapshot();
        pred.setUserId(current.getUserId());
        pred.setDate(LocalDate.now().plusDays(daysAhead));
        pred.setBranches(predBranches);
        pred.setPrediction(Prediction.builder()
                .optimistic(Scenario.builder().date(LocalDate.now().plusDays(daysAhead))
                        .totalBranches(predBranches.size() + newBranches).avgMastery(avgMastery)
                        .description("").build())
                .pessimistic(null).build());
        return pred;
    }

    private KnowledgeTreeSnapshot buildPessimisticPrediction(KnowledgeTreeSnapshot current, int daysAhead) {
        List<Branch> predBranches = new ArrayList<>();
        for (Branch b : current.getBranches()) {
            double retained = ForgettingCurveUtil.predictRetention(b.getProgress(), 0, daysAhead);
            predBranches.add(Branch.builder()
                    .name(b.getName()).progress(retained)
                    .stability(Math.max(0.1, b.getStability() - 0.15))
                    .predictedRetention30d(ForgettingCurveUtil.predictRetention(retained, daysAhead, 30))
                    .nodes(b.getNodes()).build());
        }
        double avgMastery = predBranches.stream().mapToDouble(Branch::getProgress).average().orElse(0);

        KnowledgeTreeSnapshot pred = new KnowledgeTreeSnapshot();
        pred.setUserId(current.getUserId());
        pred.setDate(LocalDate.now().plusDays(daysAhead));
        pred.setBranches(predBranches);
        pred.setPrediction(Prediction.builder()
                .pessimistic(Scenario.builder().date(LocalDate.now().plusDays(daysAhead))
                        .totalBranches(predBranches.size()).avgMastery(avgMastery)
                        .description("").build())
                .optimistic(null).build());
        return pred;
    }

}
