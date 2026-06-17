package com.jellystudy.companion.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 错误码枚举
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    SPIRIT_NOT_FOUND(10001, "精灵不存在"),
    SPIRIT_ALREADY_SLEEPING(10002, "精灵已在沉睡中"),
    FEYNMAN_SESSION_NOT_FOUND(10003, "费曼教学会话不存在"),
    FEYNMAN_MAX_ROUNDS_EXCEEDED(10004, "超过最大追问轮数"),
    AI_CALL_FAILED(10005, "AI调用失败"),
    KNOWLEDGE_SERVICE_UNAVAILABLE(10006, "知识点服务不可用"),
    STUDY_PLAN_SERVICE_UNAVAILABLE(10007, "学习计划服务不可用");

    private final int code;
    private final String message;
}
