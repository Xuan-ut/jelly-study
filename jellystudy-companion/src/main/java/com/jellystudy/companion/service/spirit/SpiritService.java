package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;

/**
 * 精灵系统主服务接口
 */
public interface SpiritService {

    SpiritState getSpiritState(Long userId);

    SpiritGreetingResult getGreeting(Long userId);

    SpiritChatResult chat(Long userId, String sessionId, String message);

    SpiritState feed(Long userId, String eventType, int feedValue);

    /** 重命名精灵 */
    SpiritState renameSpirit(Long userId, String newName);

    /** 切换精灵性格 */
    SpiritState switchPersonality(Long userId, String personalityKey, String personalityPrompt);

    /** 精灵主动发起聊天 */
    SpiritChatResult proactiveChat(Long userId);

    /** 切换精灵外形（只能选择已解锁等级及以下的外形） */
    SpiritState switchAppearance(Long userId, int targetLevel);

    /** 获取今日学习计划与每日任务 */
    TodayTasksResult getTodayTasks(Long userId);

    /** 切换每日任务完成状态并给予AI鼓励 */
    TaskToggleResult toggleTask(Long userId, String taskId, String date);

    /** 快捷生成学习计划（根据用户输入的主题用AI生成） */
    QuickPlanResult quickGeneratePlan(Long userId, String topic);

    /** 番茄钟计时扣减饱食度（每分钟扣1点） */
    SpiritState pomodoroTick(Long userId, int minutes);

    /** 今日任务结果 */
    class TodayTasksResult {
        private java.util.List<java.util.Map<String, Object>> plans;
        private java.util.List<java.util.Map<String, Object>> tasks;
        private int completedCount;
        private int totalCount;

        public TodayTasksResult(java.util.List<java.util.Map<String, Object>> plans,
                                java.util.List<java.util.Map<String, Object>> tasks,
                                int completedCount, int totalCount) {
            this.plans = plans;
            this.tasks = tasks;
            this.completedCount = completedCount;
            this.totalCount = totalCount;
        }
        public java.util.List<java.util.Map<String, Object>> getPlans() { return plans; }
        public java.util.List<java.util.Map<String, Object>> getTasks() { return tasks; }
        public int getCompletedCount() { return completedCount; }
        public int getTotalCount() { return totalCount; }
    }

    /** 任务切换结果 */
    class TaskToggleResult {
        private boolean completed;
        private String encouragement;
        private String emotion;

        public TaskToggleResult(boolean completed, String encouragement, String emotion) {
            this.completed = completed;
            this.encouragement = encouragement;
            this.emotion = emotion;
        }
        public boolean isCompleted() { return completed; }
        public String getEncouragement() { return encouragement; }
        public String getEmotion() { return emotion; }
    }

    /** 快捷生成计划结果 */
    class QuickPlanResult {
        private boolean success;
        private String planId;
        private String planTitle;
        private String message;

        public QuickPlanResult(boolean success, String planId, String planTitle, String message) {
            this.success = success;
            this.planId = planId;
            this.planTitle = planTitle;
            this.message = message;
        }
        public boolean isSuccess() { return success; }
        public String getPlanId() { return planId; }
        public String getPlanTitle() { return planTitle; }
        public String getMessage() { return message; }
    }

    /** 问候结果 */
    class SpiritGreetingResult {
        private String emotion;
        private String greeting;
        private String suggestion;

        public SpiritGreetingResult(String emotion, String greeting, String suggestion) {
            this.emotion = emotion;
            this.greeting = greeting;
            this.suggestion = suggestion;
        }
        public String getEmotion() { return emotion; }
        public String getGreeting() { return greeting; }
        public String getSuggestion() { return suggestion; }
    }

    /** 对话结果 */
    class SpiritChatResult {
        private String sessionId;
        private String spiritMessage;
        private String emotion;
        /** 意图类型：normal / generate_plan / generated_plan */
        private String intent;
        /** 已生成计划的id（仅 intent=generated_plan 时有值） */
        private String planId;
        /** 已生成计划的标题 */
        private String planTitle;

        public SpiritChatResult(String sessionId, String spiritMessage, String emotion) {
            this.sessionId = sessionId;
            this.spiritMessage = spiritMessage;
            this.emotion = emotion;
            this.intent = "normal";
        }
        public String getSessionId() { return sessionId; }
        public String getSpiritMessage() { return spiritMessage; }
        public String getEmotion() { return emotion; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
        public String getPlanId() { return planId; }
        public void setPlanId(String planId) { this.planId = planId; }
        public String getPlanTitle() { return planTitle; }
        public void setPlanTitle(String planTitle) { this.planTitle = planTitle; }
    }
}
