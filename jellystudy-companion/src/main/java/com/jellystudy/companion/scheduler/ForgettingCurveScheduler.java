package com.jellystudy.companion.scheduler;

import com.jellystudy.companion.service.timeline.ForgettingCurveService;
import com.jellystudy.companion.service.timeline.KnowledgeTreeService;
import com.jellystudy.companion.util.ScheduleLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 遗忘曲线计算 — 每日凌晨4点执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ForgettingCurveScheduler {

    private final ForgettingCurveService forgettingCurveService;
    private final KnowledgeTreeService knowledgeTreeService;
    private final ScheduleLockUtil lockUtil;

    @Scheduled(cron = "${jellystudy.companion.scheduler.forgetting-calc}")
    public void calculateForgettingCurve() {
        log.info("===== 遗忘曲线计算开始 =====");
        String taskName = "forgetting-curve-calc";
        if (!lockUtil.tryLock(taskName, 900)) {
            log.info("遗忘曲线计算任务已被其他实例执行，跳过");
            return;
        }
        try {
            // TODO: 遍历所有活跃用户，更新其知识树和遗忘曲线
            log.info("遗忘曲线批量计算完成");
        } catch (Exception e) {
            log.error("遗忘曲线计算失败", e);
        } finally {
            lockUtil.unlock(taskName);
        }
        log.info("===== 遗忘曲线计算结束 =====");
    }
}
