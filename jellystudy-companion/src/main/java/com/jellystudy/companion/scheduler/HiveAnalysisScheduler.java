package com.jellystudy.companion.scheduler;

import com.jellystudy.companion.service.hive.PatternDiscoveryService;
import com.jellystudy.companion.util.ScheduleLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 群体模式分析 — 每日凌晨3点执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HiveAnalysisScheduler {

    private final PatternDiscoveryService patternDiscoveryService;
    private final ScheduleLockUtil lockUtil;

    @Scheduled(cron = "${jellystudy.companion.scheduler.hive-analysis}")
    public void analyzeHivePatterns() {
        log.info("===== 群体模式分析开始 =====");
        String taskName = "hive-pattern-analysis";
        if (!lockUtil.tryLock(taskName, 1800)) {
            log.info("群体模式分析任务已被其他实例执行，跳过");
            return;
        }
        try {
            patternDiscoveryService.discoverPatterns();
        } catch (Exception e) {
            log.error("群体模式分析失败", e);
        } finally {
            lockUtil.unlock(taskName);
        }
        log.info("===== 群体模式分析结束 =====");
    }
}
