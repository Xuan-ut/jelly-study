package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.enums.SpiritEmotion;

/**
 * 精灵系统主服务接口
 */
public interface SpiritService {

    SpiritState getSpiritState(Long userId);

    SpiritGreetingResult getGreeting(Long userId);

    SpiritChatResult chat(Long userId, String sessionId, String message);

    SpiritState feed(Long userId, String eventType, int feedValue);

    /** 重命名精灵 */
    SpiritState renameSpirit(Long userId, String newName);

    /** 切换精灵性格 */
    SpiritState switchPersonality(Long userId, String personalityKey, String personalityPrompt);

    /** 精灵主动发起聊天 */
    SpiritChatResult proactiveChat(Long userId);

    /** 切换精灵外形（只能选择已解锁等级及以下的外形） */
    SpiritState switchAppearance(Long userId, int targetLevel);

    /** 问候结果 */
    class SpiritGreetingResult {
        private String emotion;
        private String greeting;
        private String suggestion;

        public SpiritGreetingResult(String emotion, String greeting, String suggestion) {
            this.emotion = emotion;
            this.greeting = greeting;
            this.suggestion = suggestion;
        }
        public String getEmotion() { return emotion; }
        public String getGreeting() { return greeting; }
        public String getSuggestion() { return suggestion; }
    }

    /** 对话结果 */
    class SpiritChatResult {
        private String sessionId;
        private String spiritMessage;
        private String emotion;

        public SpiritChatResult(String sessionId, String spiritMessage, String emotion) {
            this.sessionId = sessionId;
            this.spiritMessage = spiritMessage;
            this.emotion = emotion;
        }
        public String getSessionId() { return sessionId; }
        public String getSpiritMessage() { return spiritMessage; }
        public String getEmotion() { return emotion; }
    }
}
