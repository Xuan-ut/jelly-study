package com.jellystudy.companion.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jellystudy.companion.ai.model.AssessmentResult;
import com.jellystudy.companion.ai.model.ChatRequest;
import com.jellystudy.companion.ai.model.ChatResponse;
import com.jellystudy.companion.config.LLMConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * AI 大模型 API 客户端
 * 封装 MiniMax API 调用，支持普通对话和结构化评估
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class AIClient {

    private final RestTemplate restTemplate;
    private final LLMConfig llmConfig;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    /**
     * 调用 AI 进行普通对话
     */
    public String chat(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        try {
            ChatRequest request = ChatRequest.builder()
                    .model(llmConfig.getModel())
                    .messages(List.of(
                            ChatRequest.Message.of("system", systemPrompt),
                            ChatRequest.Message.of("user", userMessage)
                    ))
                    .temperature(llmConfig.getTemperature())
                    .maxTokens(llmConfig.getMaxTokens())
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

            HttpEntity<ChatRequest> entity = new HttpEntity<>(request, headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    llmConfig.getBaseUrl(), HttpMethod.POST, entity, String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("AI调用成功, 耗时: {}ms", elapsed);

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode messageNode = choices.get(0).get("message");
                if (messageNode != null && messageNode.has("content")) {
                    return messageNode.get("content").asText().trim();
                }
            }
            log.warn("AI返回格式异常: {}", response.getBody());
            return "我好像走神了一下...你能再说一遍吗？";
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI调用失败, 耗时: {}ms, error: {}", elapsed, e.getMessage());
            return getFallbackResponse();
        }
    }

    /**
     * 调用 AI 并解析为结构化评估结果
     */
    public AssessmentResult chatForAssessment(String systemPrompt, String userMessage) {
        long start = System.currentTimeMillis();
        try {
            String responseText = chat(systemPrompt, userMessage);
            // 尝试从响应中提取 JSON
            String json = extractJson(responseText);
            return objectMapper.readValue(json, AssessmentResult.class);
        } catch (JsonProcessingException e) {
            log.warn("AI评估结果JSON解析失败, 使用默认评估: {}", e.getMessage());
            return buildDefaultAssessment();
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("AI评估调用失败, 耗时: {}ms, error: {}", elapsed, e.getMessage());
            return buildDefaultAssessment();
        }
    }

    /**
     * 从AI返回文本中提取JSON
     */
    private String extractJson(String text) {
        int startIdx = text.indexOf("{");
        int endIdx = text.lastIndexOf("}");
        if (startIdx >= 0 && endIdx > startIdx) {
            return text.substring(startIdx, endIdx + 1);
        }
        return text;
    }

    /**
     * AI调用失败时的兜底响应
     */
    private String getFallbackResponse() {
        return "嗯...我刚刚走神了一下，你能再说一遍吗？";
    }

    /**
     * 构建默认评估结果（用于AI调用失败时的降级）
     */
    private AssessmentResult buildDefaultAssessment() {
        return AssessmentResult.builder()
                .overallScore(0.5)
                .suggestedNextStep("AI评估暂时不可用，请稍后再试")
                .build();
    }
}
