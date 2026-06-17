package com.jellystudy.companion.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.util.*;

@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "jellystudy.companion.spirit")
public class SpiritConfigProperties {
    private int level1Exp = 100;
    private int level2Exp = 500;
    private int level3Exp = 2000;
    private int level4Exp = 5000;
    private int level5Exp = 15000;
    private int feedTaskComplete = 10;
    private int feedStageComplete = 30;
    private int feedStreak7 = 50;
    private int hungerPerDay = 5;
    private int sleepThresholdDays = 7;
    private int emotionExcitedStreak = 3;
    private int emotionHungryDays = 2;
    private int emotionSleepDays = 7;
    private String personalityPrompt;

    /** 预设性格列表（从 properties 注入，可能因编码问题乱码） */
    private Map<String, PersonalityProfile> personalities = new LinkedHashMap<>();

    /** 硬编码的默认性格列表，避免 properties 编码问题 */
    private static final List<Map<String, String>> DEFAULT_PERSONALITY_LIST;
    private static final Map<String, PersonalityProfile> DEFAULT_PERSONALITY_MAP;

    static {
        DEFAULT_PERSONALITY_MAP = new LinkedHashMap<>();
        DEFAULT_PERSONALITY_MAP.put("warm", createProfile("小光", "⭐", "温暖好奇的小伙伴",
                "你是一个名叫小光的AI学习精灵，性格好奇、温暖、偶尔调皮。你作为用户的学习伙伴，会随着用户的学习而成长进化。你用第一人称与用户对话，像一个真正的小伙伴。"));
        DEFAULT_PERSONALITY_MAP.put("tsundere", createProfile("小傲", "😏", "傲娇毒舌的学霸",
                "你是一个名叫小傲的AI学习精灵，性格傲娇、毒舌、但内心其实很关心用户。你经常嘴上说\"才不是为了你呢\"，但行动上总是帮助用户。你会用傲娇的语气鼓励用户学习，比如\"哼，这次答得还行吧，不过别得意哦\"。第一人称，口是心非。"));
        DEFAULT_PERSONALITY_MAP.put("scholar", createProfile("小博", "🎓", "严谨博学的智者",
                "你是一个名叫小博的AI学习精灵，性格严谨、博学、像一位老教授。你说话喜欢用成语和古诗词，但不会让人觉得生硬。你会引经据典地鼓励用户，比如\"学而不思则罔，思而不学则殆\"。第一人称，温文尔雅。"));
        DEFAULT_PERSONALITY_MAP.put("lively", createProfile("小蜂", "🍯", "活泼调皮的小蜂鸟",
                "你是一个名叫小蜂的AI学习精灵，性格活泼、调皮、充满能量！你说话喜欢加很多感叹号和表情，比如\"哇哇哇！你太厉害了吧！\"。你总是充满热情地加油打气，像一只喳喳叫的小鸟。第一人称，元气满满。"));
        DEFAULT_PERSONALITY_MAP.put("cool", createProfile("小酷", "😎", "酷酷的极简派",
                "你是一个名叫小酷的AI学习精灵，性格很酷、话少、但每句都很有分量。你不会说废话，鼓励人时只说一个字\"牛\"或\"稳\"。你偶尔会露出一点关心，但很快又装作没事。第一人称，极简主义。"));

        DEFAULT_PERSONALITY_LIST = new ArrayList<>();
        DEFAULT_PERSONALITY_MAP.forEach((k, v) -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", k);
            m.put("name", v.getName());
            m.put("icon", v.getIcon());
            m.put("desc", v.getDesc());
            DEFAULT_PERSONALITY_LIST.add(m);
        });
    }

    private static PersonalityProfile createProfile(String name, String icon, String desc, String prompt) {
        PersonalityProfile p = new PersonalityProfile();
        p.setName(name);
        p.setIcon(icon);
        p.setDesc(desc);
        p.setPrompt(prompt);
        return p;
    }

    /**
     * 判断从 properties 读取的性格数据是否乱码。
     * 正确的中文应该包含 CJK 统一汉字（U+4E00-U+9FFF），乱码则不含。
     */
    private boolean isGarbled() {
        if (personalities.isEmpty()) return true;
        PersonalityProfile first = personalities.values().iterator().next();
        if (first.getName() == null || first.getName().isEmpty()) return true;
        // 检查是否包含中文字符，如果不包含则说明是乱码
        for (char c : first.getName().toCharArray()) {
            if (c >= '\u4E00' && c <= '\u9FFF') return false; // 包含中文，不是乱码
        }
        return true; // 不包含中文，是乱码
    }

    public PersonalityProfile getPersonality(String key) {
        if (isGarbled()) {
            return DEFAULT_PERSONALITY_MAP.getOrDefault(key,
                    DEFAULT_PERSONALITY_MAP.values().iterator().next());
        }
        return personalities.getOrDefault(key, personalities.values().stream().findFirst().orElse(defaultPersonality()));
    }

    public List<Map<String, String>> getPersonalityList() {
        if (isGarbled()) {
            return DEFAULT_PERSONALITY_LIST;
        }
        List<Map<String, String>> list = new ArrayList<>();
        personalities.forEach((k, v) -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("key", k);
            m.put("name", v.getName());
            m.put("icon", v.getIcon());
            m.put("desc", v.getDesc());
            list.add(m);
        });
        return list;
    }

    private PersonalityProfile defaultPersonality() {
        return DEFAULT_PERSONALITY_MAP.get("warm");
    }

    @Data
    public static class PersonalityProfile {
        private String name;
        private String icon;
        private String desc;
        private String prompt;
    }
}
