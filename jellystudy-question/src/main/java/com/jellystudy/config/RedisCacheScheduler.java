package com.jellystudy.config;

import com.jellystudy.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Redis缓存定时任务调度器
 * 负责定时刷新热点问题缓存
 */
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class RedisCacheScheduler {

    private final RedisService redisService;

    /**
     * 每天早上8:30预加载热点问题（上班高峰期前）
     */
    @Scheduled(cron = "0 30 8 * * ?")
    public void preloadHotQuestionsMorning() {
        log.info("定时任务：早上8:30预加载热点问题");
        redisService.preloadHotQuestions();
    }

    /**
     * 每天晚上19:30预加载热点问题（晚间高峰期前）
     */
    @Scheduled(cron = "0 30 19 * * ?")
    public void preloadHotQuestionsEvening() {
        log.info("定时任务：晚上19:30预加载热点问题");
        redisService.preloadHotQuestions();
    }

    /**
     * 每2小时刷新一次热点问题缓存
     */
    @Scheduled(cron = "0 0 */2 * * ?")
    public void refreshHotQuestions() {
        log.info("定时任务：每2小时刷新热点问题缓存");
        redisService.preloadHotQuestions();
    }

    /**
     * 每天凌晨3:00重新计算受欢迎度排行榜
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void recalculatePopularRank() {
        log.info("定时任务：凌晨3:00重新计算受欢迎度排行榜");
        // 通过获取热门问题来触发重新计算
        redisService.getPopularQuestions(50);
    }
}
