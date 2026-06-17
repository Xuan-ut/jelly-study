package com.jellystudy.companion.util;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具
 * 用于定时任务防重执行
 */
@Component
@RequiredArgsConstructor
public class ScheduleLockUtil {

    private final StringRedisTemplate redisTemplate;

    /**
     * 尝试获取分布式锁
     */
    public boolean tryLock(String taskName, long expireSeconds) {
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent("companion:schedule:lock:" + taskName, "1",
                        expireSeconds, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(acquired);
    }

    /**
     * 释放分布式锁
     */
    public void unlock(String taskName) {
        redisTemplate.delete("companion:schedule:lock:" + taskName);
    }
}
