package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;
import com.jellystudy.companion.repository.SpiritStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 精灵情感系统
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpiritEmotionServiceImpl implements SpiritEmotionService {

    private final SpiritStateRepository spiritStateRepository;
    private final SpiritConfigProperties spiritConfig;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void updateEmotion(Long userId, SpiritEmotion newEmotion) {
        spiritStateRepository.findByUserId(userId).ifPresent(spirit -> {
            SpiritEmotion oldEmotion = spirit.getEmotion();
            if (oldEmotion != newEmotion) {
                spirit.setEmotion(newEmotion);
                spirit.setUpdateTime(LocalDateTime.now());
                spiritStateRepository.save(spirit);
                log.info("精灵情感切换: userId={}, {}→{}", userId,
                        oldEmotion.getDisplayName(), newEmotion.getDisplayName());
            }
        });
    }

    @Override
    public SpiritEmotion calculateEmotion(Long userId) {
        SpiritState spirit = spiritStateRepository.findByUserId(userId).orElse(null);
        if (spirit == null) return SpiritEmotion.CALM;

        // 基于饱食度判断（饱食度越低 = 学习越不活跃）
        // 喂食后饱食度恢复，精灵应该能从沉睡/饥饿中恢复
        int satiation = spirit.getSatiation();
        if (satiation <= 20) {
            return SpiritEmotion.SLEEPING;
        } else if (satiation <= 40) {
            return SpiritEmotion.HUNGRY;
        } else if (satiation <= 60) {
            return SpiritEmotion.CALM;
        } else if (satiation <= 85) {
            return SpiritEmotion.HAPPY;
        } else {
            return SpiritEmotion.EXCITED;
        }
    }

    @Override
    public void updateEmotionsBasedOnHunger() {
        // 检查所有饥饿状态的精灵，如果饥饿持续时间过长，降级为沉睡
        List<SpiritState> hungrySpirits = spiritStateRepository.findByEmotion(SpiritEmotion.HUNGRY);
        for (SpiritState spirit : hungrySpirits) {
            int newSatiation = Math.max(0, spirit.getSatiation() - spiritConfig.getHungerPerDay());
            spirit.setSatiation(newSatiation);
            if (newSatiation <= 20) {
                spirit.setEmotion(SpiritEmotion.SLEEPING);
                log.info("精灵进入沉睡: userId={}, satiation={}", spirit.getUserId(), newSatiation);
            }
            spirit.setUpdateTime(LocalDateTime.now());
            spiritStateRepository.save(spirit);
            // 清除Redis缓存，避免缓存和DB不一致
            redisTemplate.delete(RedisKeyConstants.spiritState(spirit.getUserId()));
        }

        // 检查所有CALM状态，看是否需要降级为HUNGRY
        List<SpiritState> calmSpirits = spiritStateRepository.findByEmotion(SpiritEmotion.CALM);
        for (SpiritState spirit : calmSpirits) {
            int newSatiation = Math.max(0, spirit.getSatiation() - spiritConfig.getHungerPerDay());
            spirit.setSatiation(newSatiation);
            if (newSatiation <= 40) {
                spirit.setEmotion(SpiritEmotion.HUNGRY);
                log.info("精灵进入饥饿: userId={}, satiation={}", spirit.getUserId(), newSatiation);
            }
            spirit.setUpdateTime(LocalDateTime.now());
            spiritStateRepository.save(spirit);
            // 清除Redis缓存，避免缓存和DB不一致
            redisTemplate.delete(RedisKeyConstants.spiritState(spirit.getUserId()));
        }
        log.info("情感批量更新完成: 处理HUNGRY={}个, CALM={}个", hungrySpirits.size(), calmSpirits.size());
    }
}
