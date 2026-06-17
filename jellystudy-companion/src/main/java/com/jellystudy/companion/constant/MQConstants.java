package com.jellystudy.companion.constant;

/**
 * RabbitMQ 常量管理
 */
public final class MQConstants {

    // ===== 交换机 =====
    public static final String EXCHANGE_LEARNING_EVENT = "jellystudy.learning.event";

    // ===== 队列 =====
    public static final String QUEUE_SPIRIT_FEED = "companion.spirit.feed";
    public static final String QUEUE_HIVE_COLLECT = "companion.hive.collect";
    public static final String QUEUE_SPIRIT_NOTIFY = "companion.spirit.notify";

    // ===== 路由键 =====
    public static final String RK_TASK_COMPLETED = "task.completed";
    public static final String RK_STAGE_COMPLETED = "stage.completed";
    public static final String RK_PLAN_CREATED = "plan.created";
    public static final String RK_PROGRESS_UPDATED = "progress.updated";
    public static final String RK_SPIRIT_LEVELUP = "spirit.levelup";
    public static final String RK_SPIRIT_EMOTION_CHANGE = "spirit.emotion.change";
}
