package com.jellystudy.companion.service.spirit;

import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritChatResult;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritGreetingResult;

/**
 * 精灵对话系统接口
 */
public interface SpiritChatService {

    /** 生成精灵问候语 */
    SpiritGreetingResult getGreeting(SpiritState spirit);

    /** 精灵多轮对话 */
    SpiritChatResult chat(SpiritState spirit, String sessionId, String message);

    /** 精灵主动发起聊天 */
    SpiritChatResult proactiveChat(SpiritState spirit);
}
