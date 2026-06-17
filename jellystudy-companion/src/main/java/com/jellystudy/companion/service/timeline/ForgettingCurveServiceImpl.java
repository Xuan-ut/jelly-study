package com.jellystudy.companion.service.timeline;

import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Branch;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Node;
import com.jellystudy.companion.repository.KnowledgeTreeSnapshotRepository;
import com.jellystudy.companion.util.ForgettingCurveUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * 遗忘曲线服务
 * 基于艾宾浩斯公式 R = e^(-t/S)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ForgettingCurveServiceImpl implements ForgettingCurveService {

    private final KnowledgeTreeSnapshotRepository snapshotRepository;

    @Override
    public void calculateAllRetentions(Long userId) {
        KnowledgeTreeSnapshot snapshot = snapshotRepository
                .findTopByUserIdOrderByDateDesc(userId).orElse(null);
        if (snapshot == null) return;

        LocalDate today = LocalDate.now();
        for (Branch branch : snapshot.getBranches()) {
            if (branch.getNodes() != null) {
                for (Node node : branch.getNodes()) {
                    int daysSinceReview = node.getLastReview() != null
                            ? (int) ChronoUnit.DAYS.between(node.getLastReview(), today)
                            : 0;
                    double retention = calculateNodeRetention(node.getMastery(), daysSinceReview);
                    // 更新节点的预测保留率（这里只做计算，由调用方决定是否持久化）
                    log.debug("node={}, mastery={}, daysSinceReview={}, retention={}",
                            node.getName(), node.getMastery(), daysSinceReview, retention);
                }
            }
            // 更新分支的30天预测保留率
            double avgMastery = branch.getNodes() != null && !branch.getNodes().isEmpty()
                    ? branch.getNodes().stream().mapToDouble(Node::getMastery).average().orElse(0.5)
                    : 0.5;
            branch.setPredictedRetention30d(ForgettingCurveUtil.predictRetention(avgMastery, 0, 30));
        }
        snapshotRepository.save(snapshot);
        log.info("遗忘曲线计算完成: userId={}, branches={}", userId, snapshot.getBranches().size());
    }

    @Override
    public double calculateNodeRetention(double mastery, int daysSinceLastReview) {
        double stability = ForgettingCurveUtil.calculateStability(mastery);
        return ForgettingCurveUtil.calculateRetention(daysSinceLastReview, stability);
    }
}
