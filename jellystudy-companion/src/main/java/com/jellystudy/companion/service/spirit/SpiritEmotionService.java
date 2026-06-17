package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.enums.SpiritEmotion;

/**
 * 精灵情感系统接口
 */
public interface SpiritEmotionService {

    /** 更新精灵情感状态 */
    void updateEmotion(Long userId, SpiritEmotion newEmotion);

    /** 根据用户行为计算当前情感 */
    SpiritEmotion calculateEmotion(Long userId);

    /** 定时批量更新（检查饥饿→沉睡降级） */
    void updateEmotionsBasedOnHunger();
}
