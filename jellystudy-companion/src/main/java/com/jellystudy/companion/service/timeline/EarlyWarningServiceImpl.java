package com.jellystudy.companion.service.timeline;

import com.jellystudy.companion.dubbo.consumer.StudyPlanServiceConsumer;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Branch;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Node;
import com.jellystudy.companion.enums.HivePatternType;
import com.jellystudy.companion.repository.HivePatternRepository;
import com.jellystudy.entity.StudyPlan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class EarlyWarningServiceImpl implements EarlyWarningService {

    private final KnowledgeTreeService knowledgeTreeService;
    private final HivePatternRepository hivePatternRepository;
    private final StudyPlanServiceConsumer studyPlanServiceConsumer;

    @Override
    public List<TimelineService.EarlyWarning> checkWarnings(Long userId) {
        List<TimelineService.EarlyWarning> warnings = new ArrayList<>();

        KnowledgeTreeSnapshot snapshot = knowledgeTreeService.getLatestSnapshot(userId);
        if (snapshot == null || snapshot.getBranches() == null) {
            return warnings;
        }

        // 1. 检查每个分支中保留率过低的节点
        for (Branch branch : snapshot.getBranches()) {
            if (branch.getNodes() != null) {
                for (Node node : branch.getNodes()) {
                    if (node.getMastery() < 0.3) {
                        warnings.add(buildWarning("LOW_MASTERY", "WARNING",
                                node.getName(),
                                String.format("'%s'的掌握度仅为%.0f%%，急需复习", node.getName(), node.getMastery() * 100),
                                0.6,
                                "建议通过费曼教学重新理解该知识点"));
                    }
                }
            }
            // 检查30天遗忘风险
            if (branch.getPredictedRetention30d() < 0.35) {
                warnings.add(buildWarning("FORGETTING_RISK", "WARNING",
                        branch.getName(),
                        String.format("'%s'分支30天后预计仅保留%.0f%%", branch.getName(), branch.getPredictedRetention30d() * 100),
                        0.5,
                        "在未来几天安排该分支的系统复习"));
            }
        }

        // 2. 检查学习活跃度
        List<StudyPlan> plans = studyPlanServiceConsumer.getUserPlans(userId);
        if (plans != null) {
            long activePlans = plans.stream().filter(p -> "IN_PROGRESS".equals(p.getStatus())).count();
            if (activePlans == 0 && !plans.isEmpty()) {
                warnings.add(buildWarning("NO_ACTIVE_PLAN", "INFO",
                        "学习计划",
                        "你当前没有进行中的学习计划，建议创建新计划保持学习节奏",
                        0.7,
                        "创建一个新的学习计划继续进步"));
            }
        }

        // 3. 匹配已知瓶颈模式
        List<HivePattern> bottlenecks = hivePatternRepository.findByType(HivePatternType.BOTTLENECK);
        for (HivePattern pattern : bottlenecks) {
            if (pattern.getConfidence() < 0.7) continue;
            for (Branch branch : snapshot.getBranches()) {
                boolean matches = branch.getNodes() != null && branch.getNodes().stream()
                        .anyMatch(n -> pattern.getKnowledgeId() != null
                                && n.getName().contains(pattern.getKnowledgeId()));
                if (matches) {
                    warnings.add(buildWarning("BOTTLENECK_PATTERN", "INFO",
                            pattern.getKnowledgeId(),
                            pattern.getDescription(),
                            pattern.getConfidence(),
                            "参考成功用户的学习路径提前准备补充材料"));
                }
            }
        }

        // 4. 如果知识树为空，引导用户开始学习
        if (snapshot.getBranches().isEmpty()) {
            warnings.add(buildWarning("EMPTY_TREE", "INFO",
                    "知识树",
                    "你的知识树尚未生长，开始学习吧",
                    1.0,
                    "从创建一个学习计划开始"));
        }

        return warnings;
    }

    private TimelineService.EarlyWarning buildWarning(String type, String severity,
                                                       String knowledgePoint, String description,
                                                       double affectedRate, String action) {
        TimelineService.EarlyWarning w = new TimelineService.EarlyWarning();
        w.setWarningId(UUID.randomUUID().toString().substring(0, 8));
        w.setType(type);
        w.setSeverity(severity);
        w.setKnowledgePoint(knowledgePoint);
        w.setDescription(description);
        w.setAffectedUserRate(affectedRate);
        w.setRecommendedAction(action);
        return w;
    }
}
