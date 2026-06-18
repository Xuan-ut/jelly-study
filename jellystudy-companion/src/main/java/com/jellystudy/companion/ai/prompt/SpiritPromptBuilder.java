package com.jellystudy.companion.ai.prompt;

import com.jellystudy.companion.config.SpiritConfigProperties;
import com.jellystudy.companion.entity.SpiritState;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 精灵对话 Prompt 构建器
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class SpiritPromptBuilder {

    private final SpiritConfigProperties spiritConfig;

    public String buildGreetingPrompt(SpiritState spirit, UserContext userContext) {
        return String.format(
                "%s\n\n" +
                "当前精灵状态：\n" +
                "- 昵称：%s\n" +
                "- 等级：%s (Lv.%d)\n" +
                "- 情感：%s\n" +
                "- 饱食度：%d\n" +
                "- 最近学习：%s\n\n" +
                "用户的学习上下文：\n" +
                "- 当前学习主题：%s\n" +
                "- 连续学习天数：%d\n" +
                "- 今天是否已学习：%s\n\n" +
                "请生成一段简短的问候语（30字以内），要求：\n" +
                "1. 语气与当前情感状态一致\n" +
                "2. 如果用户今天还没学习，温和提醒\n" +
                "3. 提到用户最近学习的内容，显示你记得\n" +
                "4. 第一人称，有性格，不要模板化",
                spiritConfig.getPersonalityPrompt(),
                spirit.getName(),
                spirit.getLevelName(), spirit.getLevel(),
                spirit.getEmotion().getDisplayName(),
                spirit.getSatiation(),
                userContext.getLastStudyTopic(),
                userContext.getCurrentTopic(),
                userContext.getStreakDays(),
                userContext.isStudiedToday() ? "是" : "否"
        );
    }

    public String buildChatPrompt(SpiritState spirit, List<ChatMessage> history, String userMessage) {
        String personality = spirit.getPersonalityPrompt() != null
                ? spirit.getPersonalityPrompt() : spiritConfig.getPersonalityPrompt();
        StringBuilder sb = new StringBuilder();
        sb.append(personality).append("\n\n");
        sb.append(String.format("当前精灵状态：%s(Lv.%d)，情感：%s\n\n",
                spirit.getLevelName(), spirit.getLevel(),
                spirit.getEmotion().getDisplayName()));
        sb.append("以下是最近的对话记录：\n");
        for (ChatMessage msg : history) {
            sb.append(msg.getRole()).append(": ").append(msg.getContent()).append("\n");
        }
        sb.append("\n用户说: ").append(userMessage).append("\n");
        sb.append("请以小光（AI学习精灵）的身份回复用户。要求：\n");
        sb.append("1. 保持好奇、温暖、偶尔调皮的风格\n");
        sb.append("2. 直接回答用户的问题，不要说「让我想想」之类的拖延话\n");
        sb.append("3. 回复简洁有力，不超过100字\n");
        sb.append("4. 如果用户问学习相关问题，给出具体建议\n");
        sb.append("5. 语气自然，像朋友聊天一样");
        return sb.toString();
    }

    @Data
    public static class UserContext {
        private String currentTopic;
        private String lastStudyTopic;
        private int streakDays;
        private boolean studiedToday;
    }

    @Data
    public static class ChatMessage {
        private String role;
        private String content;
    }
}
