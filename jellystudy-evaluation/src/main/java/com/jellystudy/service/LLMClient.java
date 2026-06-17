package com.jellystudy.service;

import com.jellystudy.config.LLMConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class LLMClient {

    @Autowired
    private LLMConfig llmConfig;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    public String callLLM(String prompt) {
        String provider = llmConfig.getProvider();
        
        if (provider == null || provider.isEmpty() || "mock".equalsIgnoreCase(provider)) {
            System.out.println("[LLM] Using mock mode for prompt: " + prompt.substring(0, Math.min(50, prompt.length())) + "...");
            return generateMockResponse(prompt);
        }

        try {
            System.out.println("[LLM] Calling " + provider + " with prompt: " + prompt.substring(0, Math.min(50, prompt.length())) + "...");
            switch (provider.toLowerCase()) {
                case "openai":
                    return callOpenAI(prompt);
                case "zhipu":
                    return callZhipuAI(prompt);
                case "doubao":
                    return callDoubao(prompt);
                case "qwen":
                    return callQwen(prompt);
                case "minimax":
                    return callMinimax(prompt);
                default:
                    return generateMockResponse(prompt);
            }
        } catch (Exception e) {
            System.err.println("[LLM] Error calling " + provider + ": " + e.getMessage());
            e.printStackTrace();
            return generateMockResponse(prompt);
        }
    }

    private String callOpenAI(String prompt) throws Exception {
        String url = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://api.openai.com/v1/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel() != null ? llmConfig.getModel() : "gpt-3.5-turbo");
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
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
        String result = root.get("choices").get(0).get("message").get("content").asText().trim();
        System.out.println("[LLM] OpenAI response: " + result.substring(0, Math.min(100, result.length())) + "...");
        return result;
    }

    private String callZhipuAI(String prompt) throws Exception {
        String url = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://open.bigmodel.cn/api/paas/v4/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel() != null ? llmConfig.getModel() : "glm-4");
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
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
        String result = root.get("choices").get(0).get("message").get("content").asText().trim();
        System.out.println("[LLM] ZhipuAI response: " + result.substring(0, Math.min(100, result.length())) + "...");
        return result;
    }

    private String callDoubao(String prompt) throws Exception {
        String url = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://ark.cn-beijing.volces.com/api/text/v1/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel() != null ? llmConfig.getModel() : "ep-20240822001");
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());
        
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
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
        String result = root.get("choices").get(0).get("message").get("content").asText().trim();
        System.out.println("[LLM] Doubao response: " + result.substring(0, Math.min(100, result.length())) + "...");
        return result;
    }

    private String callQwen(String prompt) throws Exception {
        String url = llmConfig.getBaseUrl() != null ? llmConfig.getBaseUrl() : "https://dashscope.aliyuncs.com/api/text/v1/chat/completions";
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmConfig.getModel() != null ? llmConfig.getModel() : "qwen-turbo");
        requestBody.put("temperature", llmConfig.getTemperature());
        requestBody.put("max_tokens", llmConfig.getMaxTokens());
        
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + llmConfig.getApiKey());
        headers.set("X-DashScope-SDK-Type", "spring-boot");

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("[LLM] Qwen raw response: " + response.getBody());
            
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.has("output")) {
                String result = root.get("output").get("choices").get(0).get("message").get("content").asText().trim();
                System.out.println("[LLM] Qwen response: " + result.substring(0, Math.min(100, result.length())) + "...");
                return result;
            } else if (root.has("choices")) {
                String result = root.get("choices").get(0).get("message").get("content").asText().trim();
                System.out.println("[LLM] Qwen response (OpenAI format): " + result.substring(0, Math.min(100, result.length())) + "...");
                return result;
            } else {
                System.err.println("[LLM] Qwen response format unknown: " + response.getBody());
                throw new Exception("Unknown response format");
            }
        } catch (RestClientException e) {
            System.err.println("[LLM] Qwen API call failed: " + e.getMessage());
            throw e;
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
        
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            System.out.println("[LLM] MiniMax raw response: " + response.getBody());
            
            JsonNode root = objectMapper.readTree(response.getBody());
            
            if (root.has("choices") && root.get("choices").size() > 0) {
                String result = root.get("choices").get(0).get("message").get("content").asText().trim();
                System.out.println("[LLM] MiniMax response: " + result.substring(0, Math.min(100, result.length())) + "...");
                return result;
            } else if (root.has("reply")) {
                String result = root.get("reply").asText().trim();
                System.out.println("[LLM] MiniMax response (reply format): " + result.substring(0, Math.min(100, result.length())) + "...");
                return result;
            } else {
                System.err.println("[LLM] MiniMax response format unknown: " + response.getBody());
                throw new Exception("Unknown response format");
            }
        } catch (RestClientException e) {
            System.err.println("[LLM] MiniMax API call failed: " + e.getMessage());
            throw e;
        }
    }

    private String generateMockResponse(String prompt) {
        if (prompt.contains("知识点") || prompt.contains("提取")) {
            return "[\"学习方法\", \"英语语法\", \"词汇积累\", \"阅读理解\"]";
        } else if (prompt.contains("难度") || prompt.contains("分级")) {
            String[] difficulties = {"简单", "中等", "困难"};
            String diff = difficulties[new Random().nextInt(difficulties.length)];
            return "{\"difficulty\": \"" + diff + "\", \"reason\": \"基于问题复杂度评估\"}";
        } else if (prompt.contains("评分") || prompt.contains("打分")) {
            int score = new Random().nextInt(30) + 60;
            String feedback = "回答较为全面";
            if (score >= 90) feedback = "回答非常优秀，逻辑清晰，内容完整";
            else if (score >= 75) feedback = "回答良好，基本覆盖要点";
            else if (score >= 60) feedback = "回答及格，需要进一步完善";
            else feedback = "回答需要改进";
            return "{\"score\": " + score + ", \"feedback\": \"" + feedback + "\", \"scoreType\": \"100\"}";
        } else {
            return "{\"result\": \"success\", \"content\": \"模拟评估完成\"}";
        }
    }
}