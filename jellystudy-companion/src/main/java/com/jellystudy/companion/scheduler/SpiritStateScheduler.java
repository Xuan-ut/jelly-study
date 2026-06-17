package com.jellystudy.companion.scheduler;

import com.jellystudy.companion.service.spirit.SpiritEmotionService;
import com.jellystudy.companion.service.spirit.SpiritGrowService;
import com.jellystudy.companion.util.ScheduleLockUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 精灵状态定时更新 — 每小时执行
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SpiritStateScheduler {

    private final SpiritEmotionService emotionService;
    private final SpiritGrowService growService;
    private final ScheduleLockUtil lockUtil;

    @Scheduled(cron = "${jellystudy.companion.scheduler.spirit-update}")
    public void updateSpiritStates() {
        log.info("===== 精灵状态定时更新开始 =====");
        String taskName = "spirit-state-update";
        if (!lockUtil.tryLock(taskName, 300)) {
            log.info("精灵状态更新任务已被其他实例执行，跳过");
            return;
        }
        try {
            emotionService.updateEmotionsBasedOnHunger();
        } catch (Exception e) {
            log.error("精灵状态更新失败", e);
        } finally {
            lockUtil.unlock(taskName);
        }
        log.info("===== 精灵状态定时更新结束 =====");
    }
}
