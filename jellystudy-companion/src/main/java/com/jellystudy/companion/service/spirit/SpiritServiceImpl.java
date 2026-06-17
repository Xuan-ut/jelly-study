package com.jellystudy.companion.service.spirit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.config.CacheConfigProperties;
import com.jellystudy.companion.constant.RedisKeyConstants;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;
import com.jellystudy.companion.repository.SpiritStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 精灵系统主服务
 * 缓存优先模式: Redis → MongoDB → 回填缓存
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpiritServiceImpl implements SpiritService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    private final SpiritStateRepository spiritStateRepository;
    private final StringRedisTemplate redisTemplate;
    private final SpiritGrowService spiritGrowService;
    private final SpiritEmotionService emotionService;
    private final SpiritChatService chatService;
    private final CacheConfigProperties cacheConfig;

    @Override
    @SneakyThrows
    public SpiritState getSpiritState(Long userId) {
        String key = RedisKeyConstants.spiritState(userId);
        String cached = redisTemplate.opsForValue().get(key);
        if (StringUtils.hasText(cached)) {
            log.debug("精灵状态命中缓存: userId={}", userId);
            return objectMapper.readValue(cached, SpiritState.class);
        }
        SpiritState state = spiritStateRepository.findByUserId(userId)
                .orElseGet(() -> spiritGrowService.initSpiritState(userId));
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(state),
                cacheConfig.getSpiritTtl(), TimeUnit.MINUTES);
        return state;
    }

    @Override
    public SpiritGreetingResult getGreeting(Long userId) {
        SpiritState spirit = getSpiritState(userId);
        SpiritEmotion currentEmotion = emotionService.calculateEmotion(userId);
        // 如果计算出的情感与缓存不一致，更新到数据库并刷新缓存
        if (spirit.getEmotion() != currentEmotion) {
            spirit.setEmotion(currentEmotion);
            spiritStateRepository.save(spirit);
            String key = RedisKeyConstants.spiritState(userId);
            redisTemplate.delete(key);
            spirit = getSpiritState(userId);
        }
        return chatService.getGreeting(spirit);
    }

    @Override
    public SpiritChatResult chat(Long userId, String sessionId, String message) {
        SpiritState spirit = getSpiritState(userId);
        return chatService.chat(spirit, sessionId, message);
    }

    @Override
    public SpiritState feed(Long userId, String eventType, int feedValue) {
        SpiritState state = spiritGrowService.feedSpirit(userId, eventType, feedValue);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        spiritGrowService.checkAndTriggerEvolution(userId);
        return getSpiritState(userId);
    }

    @Override
    public SpiritState renameSpirit(Long userId, String newName) {
        SpiritState spirit = getSpiritState(userId);
        spirit.setName(newName);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        return getSpiritState(userId);
    }

    @Override
    public SpiritState switchPersonality(Long userId, String personalityKey, String personalityPrompt) {
        SpiritState spirit = getSpiritState(userId);
        spirit.setPersonalityKey(personalityKey);
        spirit.setPersonalityPrompt(personalityPrompt);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        log.info("精灵性格切换: userId={}, personalityKey={}", userId, personalityKey);
        return getSpiritState(userId);
    }

    @Override
    public SpiritChatResult proactiveChat(Long userId) {
        SpiritState spirit = getSpiritState(userId);
        return chatService.proactiveChat(spirit);
    }

    @Override
    public SpiritState switchAppearance(Long userId, int targetLevel) {
        SpiritState spirit = getSpiritState(userId);
        int currentLevel = spirit.getLevel() != null ? spirit.getLevel() : 0;
        // 只能选择当前等级及以下的外形
        if (targetLevel < 0 || targetLevel > currentLevel) {
            throw new IllegalArgumentException("只能选择已解锁等级的外形，当前等级: " + currentLevel);
        }
        com.jellystudy.companion.enums.SpiritLevel targetSpiritLevel =
                com.jellystudy.companion.enums.SpiritLevel.getByLevel(targetLevel);
        SpiritState.Appearance newAppearance = SpiritState.Appearance.builder()
                .body(targetSpiritLevel.getAppearanceDesc())
                .build();
        // 根据等级设置装饰
        switch (targetSpiritLevel) {
            case SPROUT:
                newAppearance.setWings(null); break;
            case YOUNG_SPIRIT:
                newAppearance.setWings("small"); break;
            case ADULT_SPIRIT:
                newAppearance.setWings("large_glowing"); break;
            case SPIRIT_MASTER:
                newAppearance.setWings("large_glowing");
                newAppearance.setAura("halo"); break;
            case SAGE:
                newAppearance.setWings("large_glowing");
                newAppearance.setAura("golden");
                newAppearance.setCrown("crown"); break;
            default: break;
        }
        spirit.setAppearance(newAppearance);
        spirit.setAppearanceLevel(targetLevel);
        spirit.setUpdateTime(java.time.LocalDateTime.now());
        spiritStateRepository.save(spirit);
        String key = RedisKeyConstants.spiritState(userId);
        redisTemplate.delete(key);
        log.info("精灵外形切换: userId={}, targetLevel={}", userId, targetLevel);
        return getSpiritState(userId);
    }
}
