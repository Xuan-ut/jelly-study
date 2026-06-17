package com.jellystudy.service;

import com.jellystudy.config.LLMConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class LLMClient {

    @Autowired
    private LLMConfig llmConfig;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String callLLM(String prompt) throws Exception {
        String provider = llmConfig.getProvider();
        
        switch (provider.toLowerCase()) {
            case "minimax":
                return callMinimax(prompt);
            default:
                return callMinimax(prompt);
        }
    }

    private String callMinimax(String prompt) throws Exception {
        String url = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://api.minimax.chat/v1/text/chatcompletion_v2";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel() != null ? llmConfig.getModel() : "abab6.5s-chat");
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray() && choices.size() > 0) {
            JsonNode messageNode = choices.get(0).get("message");
            if (messageNode != null && messageNode.has("content")) {
                return messageNode.get("content").asText().trim();
            }
        }
        
        return "未能获取到AI响应。";
    }
}