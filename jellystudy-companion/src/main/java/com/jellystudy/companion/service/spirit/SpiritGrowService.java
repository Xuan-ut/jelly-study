package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.entity.SpiritState;

/**
 * 精灵养成系统接口
 */
public interface SpiritGrowService {

    /** 喂养精灵 */
    SpiritState feedSpirit(Long userId, String eventType, int feedValue);

    /** 喂养精灵（带事件描述） */
    SpiritState feedSpirit(Long userId, String eventType, String eventDetail, int feedValue);

    /** 检查并触发进化 */
    void checkAndTriggerEvolution(Long userId);

    /** 初始化新精灵 */
    SpiritState initSpiritState(Long userId);
}
