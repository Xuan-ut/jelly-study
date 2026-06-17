package com.jellystudy.companion.constant;

/**
 * Redis Key 常量管理
 */
public final class RedisKeyConstants {

    private static final String PREFIX = "companion:";

    /** 精灵状态缓存 - Hash: companion:spirit:{userId}, TTL: 30min */
    public static final String SPIRIT_STATE = PREFIX + "spirit:%d";

    /** 知识树快照缓存 - String(JSON): companion:tree:{userId}, TTL: 60min */
    public static final String KNOWLEDGE_TREE = PREFIX + "tree:%d";

    /** 群体模式缓存 - String(JSON): companion:pattern:{subject}, TTL: 360min */
    public static final String HIVE_PATTERN = PREFIX + "pattern:%s";

    /** 群体学习排行榜 - Sorted Set: companion:leaderboard:weekly, Score=学习时长 */
    public static final String LEADERBOARD_WEEKLY = PREFIX + "leaderboard:weekly";

    /** 用户学习画像摘要 - String(JSON): companion:profile:{userId}, TTL: 30min */
    public static final String USER_PROFILE = PREFIX + "profile:%d";

    /** 精灵对话历史缓存 - List: companion:chat:history:{userId}:{sessionId} */
    public static final String CHAT_HISTORY = PREFIX + "chat:history:%d:%s";

    /** 调度任务分布式锁 - String: schedule:lock:{taskName} */
    public static final String SCHEDULE_LOCK = PREFIX + "schedule:lock:%s";

    public static String spiritState(Long userId) {
        return String.format(SPIRIT_STATE, userId);
    }

    public static String knowledgeTree(Long userId) {
        return String.format(KNOWLEDGE_TREE, userId);
    }

    public static String hivePattern(String subject) {
        return String.format(HIVE_PATTERN, subject);
    }

    public static String userProfile(Long userId) {
        return String.format(USER_PROFILE, userId);
    }

    public static String chatHistory(Long userId, String sessionId) {
        return String.format(CHAT_HISTORY, userId, sessionId);
    }

    public static String scheduleLock(String taskName) {
        return String.format(SCHEDULE_LOCK, taskName);
    }
}
