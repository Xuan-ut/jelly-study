package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.constant.SpiritConstants;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.entity.SpiritState.GrowthLog;
import com.jellystudy.companion.entity.SpiritState.Appearance;
import com.jellystudy.companion.entity.SpiritState.Memory;
import com.jellystudy.companion.enums.GrowthEventType;
import com.jellystudy.companion.enums.SpiritEmotion;
import com.jellystudy.companion.enums.SpiritLevel;
import com.jellystudy.companion.repository.SpiritStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 精灵养成系统
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpiritGrowServiceImpl implements SpiritGrowService {

    private final SpiritStateRepository spiritStateRepository;
    private final SpiritConfigProperties spiritConfig;

    @Override
    public SpiritState feedSpirit(Long userId, String eventType, int feedValue) {
        return feedSpirit(userId, eventType, null, feedValue);
    }

    @Override
    public SpiritState feedSpirit(Long userId, String eventType, String eventDetail, int feedValue) {
        SpiritState spirit = spiritStateRepository.findByUserId(userId)
                .orElseGet(() -> initSpiritState(userId));

        // 防御性处理：确保 Integer 字段不为 null
        if (spirit.getExperience() == null) spirit.setExperience(0);
        if (spirit.getSatiation() == null) spirit.setSatiation(SpiritConstants.INITIAL_SATIATION);
        if (spirit.getLevel() == null) spirit.setLevel(0);
        if (spirit.getNextLevelExp() == null) spirit.setNextLevelExp(getNextLevelExp(spirit.getLevel()));

        // 增加经验值
        int newExp = spirit.getExperience() + feedValue;
        spirit.setExperience(newExp);
        log.info("精灵喂养: userId={}, eventType={}, feedValue={}, exp={}/{}",
                userId, eventType, feedValue, newExp, spirit.getNextLevelExp());

        // 更新饱食度
        int newSatiation = Math.min(SpiritConstants.MAX_SATIATION, spirit.getSatiation() + feedValue / 2);
        spirit.setSatiation(newSatiation);

        // 根据饱食度计算新情感
        SpiritEmotion newEmotion = calculateEmotionBySatiation(newSatiation);
        SpiritEmotion oldEmotion = spirit.getEmotion();
        if (oldEmotion != newEmotion) {
            if (oldEmotion == SpiritEmotion.SLEEPING) {
                spirit.getGrowthLog().add(buildGrowthLog(GrowthEventType.AWAKENED, "精灵被唤醒", feedValue));
            }
            spirit.setEmotion(newEmotion);
            log.info("精灵情感变化: userId={}, {}→{}", userId,
                    oldEmotion.getDisplayName(), newEmotion.getDisplayName());
        }

        // 记录成长日志
        GrowthEventType growthType = mapEventType(eventType);
        String detail = eventDetail != null ? eventDetail : buildEventDetail(eventType, feedValue);
        spirit.getGrowthLog().add(buildGrowthLog(growthType, detail, feedValue));

        spirit.setUpdateTime(LocalDateTime.now());
        return spiritStateRepository.save(spirit);
    }

    /** 将MQ routing key映射为成长事件类型 */
    private GrowthEventType mapEventType(String routingKey) {
        if (routingKey == null) return GrowthEventType.MANUAL_FEED;
        if (routingKey.contains("task.completed")) return GrowthEventType.TASK_COMPLETED;
        if (routingKey.contains("stage.completed")) return GrowthEventType.STAGE_COMPLETED;
        if (routingKey.contains("plan.completed")) return GrowthEventType.PLAN_COMPLETED;
        if (routingKey.contains("stage.completed")) return GrowthEventType.STAGE_COMPLETED;
        if (routingKey.contains("daily")) return GrowthEventType.DAILY_STREAK;
        if (routingKey.contains("milestone")) return GrowthEventType.MILESTONE_REACHED;
        return GrowthEventType.MANUAL_FEED;
    }

    /** 根据事件类型生成描述 */
    private String buildEventDetail(String eventType, int feedValue) {
        if (eventType == null) return "获得" + feedValue + "经验";
        if (eventType.contains("task")) return "完成了学习任务 +" + feedValue + "exp";
        if (eventType.contains("stage")) return "完成了一个学习阶段 +" + feedValue + "exp";
        if (eventType.contains("plan")) return "完成了整个学习计划 +" + feedValue + "exp";
        if (eventType.contains("daily")) return "连续打卡 +" + feedValue + "exp";
        return "获得" + feedValue + "经验";
    }

    @Override
    public void checkAndTriggerEvolution(Long userId) {
        SpiritState spirit = spiritStateRepository.findByUserId(userId)
                .orElse(null);
        if (spirit == null) return;

        SpiritLevel currentLevel = SpiritLevel.getByLevel(spirit.getLevel());
        SpiritLevel newLevel = getLevelByExp(spirit.getExperience());

        if (newLevel.getLevel() > currentLevel.getLevel()) {
            log.info("精灵进化: userId={}, Lv.{} → Lv.{}, exp={}", userId,
                    currentLevel.getLevel(), newLevel.getLevel(), spirit.getExperience());
            spirit.setLevel(newLevel.getLevel());
            spirit.setLevelName(newLevel.getDisplayName());
            spirit.setNextLevelExp(getNextLevelExp(newLevel.getLevel()));

            // 更新外观
            Appearance appearance = getAppearanceForLevel(newLevel);
            spirit.setAppearance(appearance);
            // 进化时自动更新外形等级为新等级
            spirit.setAppearanceLevel(newLevel.getLevel());

            // 解锁新技能
            spirit.setSkills(new ArrayList<>(newLevel.getSkills()));

            // 记录成长日志
            spirit.getGrowthLog().add(buildGrowthLog(GrowthEventType.LEVEL_UP,
                    "升级为" + newLevel.getDisplayName(), 0));

            spirit.setUpdateTime(LocalDateTime.now());
            spiritStateRepository.save(spirit);
            log.info("精灵进化完成: userId={}, level={}, skills={}", userId,
                    newLevel.getDisplayName(), newLevel.getSkills());
        } else if (newLevel.getLevel() == currentLevel.getLevel()) {
            // 等级未变但 nextLevelExp 可能需要修正（修复已有数据不一致）
            int correctNextExp = getNextLevelExp(currentLevel.getLevel());
            if (spirit.getNextLevelExp() == null || spirit.getNextLevelExp() != correctNextExp) {
                spirit.setNextLevelExp(correctNextExp);
                spirit.setUpdateTime(LocalDateTime.now());
                spiritStateRepository.save(spirit);
            }
        }
    }

    @Override
    public SpiritState initSpiritState(Long userId) {
        log.info("初始化新精灵: userId={}", userId);
        SpiritLevel seedLevel = SpiritLevel.SEED;

        Appearance appearance = Appearance.builder()
                .body("seed").wings(null).aura(null).crown(null).build();

        Memory memory = Memory.builder()
                .lastInteraction(LocalDateTime.now())
                .recentTopics(new ArrayList<>())
                .userPreferences(new ArrayList<>())
                .build();

        SpiritState spirit = SpiritState.builder()
                .userId(userId)
                .name(SpiritConstants.DEFAULT_SPIRIT_NAME)
                .level(seedLevel.getLevel())
                .levelName(seedLevel.getDisplayName())
                .experience(SpiritConstants.INITIAL_EXPERIENCE)
                .nextLevelExp(spiritConfig.getLevel1Exp())
                .satiation(SpiritConstants.INITIAL_SATIATION)
                .emotion(SpiritEmotion.CALM)
                .appearance(appearance)
                .appearanceLevel(0)
                .skills(new ArrayList<>(seedLevel.getSkills()))
                .memory(memory)
                .growthLog(new ArrayList<>())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        return spiritStateRepository.save(spirit);
    }

    private int getNextLevelExp(int currentLevel) {
        switch (currentLevel) {
            case 0: return spiritConfig.getLevel1Exp();
            case 1: return spiritConfig.getLevel2Exp();
            case 2: return spiritConfig.getLevel3Exp();
            case 3: return spiritConfig.getLevel4Exp();
            case 4: return spiritConfig.getLevel5Exp();
            default: return Integer.MAX_VALUE;
        }
    }

    private Appearance getAppearanceForLevel(SpiritLevel level) {
        switch (level) {
            case SEED: return Appearance.builder().body("seed").build();
            case SPROUT: return Appearance.builder().body("sprout").wings(null).build();
            case YOUNG_SPIRIT: return Appearance.builder().body("young_spirit").wings("small").build();
            case ADULT_SPIRIT: return Appearance.builder().body("fox").wings("large_glowing").build();
            case SPIRIT_MASTER: return Appearance.builder().body("fox").wings("large_glowing").aura("halo").build();
            case SAGE: return Appearance.builder().body("fox").wings("large_glowing").aura("golden").crown("crown").build();
            default: return Appearance.builder().body("seed").build();
        }
    }

    /**
     * 根据经验值计算等级（使用配置值，而非枚举硬编码）
     */
    private SpiritLevel getLevelByExp(int exp) {
        if (exp >= spiritConfig.getLevel5Exp()) return SpiritLevel.SAGE;
        if (exp >= spiritConfig.getLevel4Exp()) return SpiritLevel.SPIRIT_MASTER;
        if (exp >= spiritConfig.getLevel3Exp()) return SpiritLevel.ADULT_SPIRIT;
        if (exp >= spiritConfig.getLevel2Exp()) return SpiritLevel.YOUNG_SPIRIT;
        if (exp >= spiritConfig.getLevel1Exp()) return SpiritLevel.SPROUT;
        return SpiritLevel.SEED;
    }

    /**
     * 根据饱食度计算情感状态
     */
    private SpiritEmotion calculateEmotionBySatiation(int satiation) {
        if (satiation <= 20) return SpiritEmotion.SLEEPING;
        if (satiation <= 40) return SpiritEmotion.HUNGRY;
        if (satiation <= 60) return SpiritEmotion.CALM;
        if (satiation <= 85) return SpiritEmotion.HAPPY;
        return SpiritEmotion.EXCITED;
    }

    private GrowthLog buildGrowthLog(GrowthEventType event, String detail, int expGained) {
        return GrowthLog.builder()
                .date(LocalDate.now())
                .event(event)
                .detail(detail)
                .expGained(expGained)
                .build();
    }
}
