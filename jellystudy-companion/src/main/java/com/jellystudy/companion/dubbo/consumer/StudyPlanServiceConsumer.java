package com.jellystudy.companion.dubbo.consumer;

import com.jellystudy.dubbo.StudyPlanDubboService;
import com.jellystudy.dubbo.StudyProgressDubboService;
import com.jellystudy.entity.DailyTaskBoard;
import com.jellystudy.entity.StudyPlan;
import com.jellystudy.entity.StudyProgress;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习计划服务 Dubbo Consumer
 */
@Component
@Slf4j
public class StudyPlanServiceConsumer {

    @DubboReference(version = "1.0.0", check = false)
    private StudyPlanDubboService studyPlanDubboService;

    @DubboReference(version = "1.0.0", check = false)
    private StudyProgressDubboService studyProgressDubboService;

    /**
     * 获取用户的学习计划列表
     */
    public List<StudyPlan> getUserPlans(Long userId) {
        try {
            return studyPlanDubboService.findByUserId(userId);
        } catch (Exception e) {
            log.error("调用学习计划服务失败: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 获取每日任务板
     */
    public DailyTaskBoard getDailyTaskBoard(Long userId, String date) {
        try {
            return studyPlanDubboService.getDailyTaskBoard(userId, date);
        } catch (Exception e) {
            log.error("调用学习计划服务失败: userId={}, date={}, error={}", userId, date, e.getMessage());
            return null;
        }
    }

    /**
     * 获取用户学习统计
     */
    public Map<String, Object> getUserStudyStats(Long userId) {
        try {
            List<StudyPlan> plans = studyPlanDubboService.findByUserId(userId);
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalPlans", plans.size());
            int completedPlans = 0;
            int totalStages = 0;
            int completedStages = 0;
            for (StudyPlan plan : plans) {
                totalStages += plan.getStages() != null ? plan.getStages().size() : 0;
                if (plan.getStages() != null) {
                    for (StudyPlan.PlanStage stage : plan.getStages()) {
                        if (stage.getProgress() >= 100) {
                            completedStages++;
                        }
                    }
                }
                if (plan.getTotalProgress() >= 100) {
                    completedPlans++;
                }
            }
            stats.put("completedPlans", completedPlans);
            stats.put("totalStages", totalStages);
            stats.put("completedStages", completedStages);
            return stats;
        } catch (Exception e) {
            log.error("调用学习计划服务失败: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 计算用户连续学习天数（从今天往回数）
     */
    public int getStreakDays(Long userId) {
        try {
            List<DailyTaskBoard> history = studyPlanDubboService.getDailyTaskBoardHistory(userId, 60);
            if (history == null || history.isEmpty()) return 0;
            // 按日期降序排列
            history.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            int streak = 0;
            java.time.LocalDate today = java.time.LocalDate.now();
            for (int i = 0; i < 60; i++) {
                java.time.LocalDate checkDate = today.minusDays(i);
                String dateStr = checkDate.toString();
                boolean hasCompleted = history.stream()
                        .filter(b -> b.getDate() != null && b.getDate().contains(dateStr))
                        .anyMatch(b -> b.getTasks() != null && b.getTasks().stream()
                                .anyMatch(t -> t.isCompleted()));
                if (hasCompleted) streak++;
                else break; // 中断则停止
            }
            return streak;
        } catch (Exception e) {
            log.error("获取连续学习天数失败: userId={}, error={}", userId, e.getMessage());
            return 0;
        }
    }

    /**
     * 检查用户今天是否已经学习过
     */
    public boolean hasStudiedToday(Long userId) {
        try {
            String todayStr = java.time.LocalDate.now().toString();
            DailyTaskBoard todayBoard = studyPlanDubboService.getDailyTaskBoard(userId, todayStr);
            if (todayBoard == null || todayBoard.getTasks() == null) return false;
            return todayBoard.getTasks().stream().anyMatch(t -> t.isCompleted());
        } catch (Exception e) {
            log.error("检查今日学习状态失败: userId={}, error={}", userId, e.getMessage());
            return false;
        }
    }

    /**
     * 获取用户学习进度记录
     */
    public List<StudyProgress> getUserProgressRecords(Long userId) {
        try {
            return studyProgressDubboService.findByUserId(userId);
        } catch (Exception e) {
            log.error("调用学习进度服务失败: userId={}, error={}", userId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
