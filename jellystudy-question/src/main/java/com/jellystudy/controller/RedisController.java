package com.jellystudy.controller;

import com.jellystudy.entity.Question;
import com.jellystudy.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Redis缓存控制器
 * 提供热门问题排行榜、常看问题缓存、热点问题预加载等功能的API接口
 */
@RestController
@RequestMapping("/api/redis")
@RequiredArgsConstructor
public class RedisController {

    private final RedisService redisService;

    // ==================== 功能1：最近最受欢迎问题排行榜 ====================

    /**
     * 获取最受欢迎问题排行榜
     * GET /api/redis/popular?topN=10
     */
    @GetMapping("/popular")
    public ResponseEntity<Map<String, Object>> getPopularQuestions(
            @RequestParam(defaultValue = "10") int topN) {
        
        List<Question> questions = redisService.getPopularQuestions(topN);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", questions);
        result.put("count", questions.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 更新问题受欢迎度分数
     * POST /api/redis/popular/update
     */
    @PostMapping("/popular/update")
    public ResponseEntity<Map<String, Object>> updatePopularScore(
            @RequestBody Map<String, Object> request) {
        
        String questionId = (String) request.get("questionId");
        int likeCount = request.get("likeCount") != null ? ((Number) request.get("likeCount")).intValue() : 0;
        int answerCount = request.get("answerCount") != null ? ((Number) request.get("answerCount")).intValue() : 0;
        int commentCount = request.get("commentCount") != null ? ((Number) request.get("commentCount")).intValue() : 0;
        
        redisService.updatePopularScore(questionId, likeCount, answerCount, commentCount, new java.util.Date());
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "受欢迎度分数更新成功");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 点赞时更新受欢迎度
     * POST /api/redis/popular/like/{questionId}
     */
    @PostMapping("/popular/like/{questionId}")
    public ResponseEntity<Map<String, Object>> onQuestionLiked(@PathVariable String questionId) {
        redisService.onQuestionLiked(questionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "点赞成功，已更新受欢迎度");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 回答时更新受欢迎度
     * POST /api/redis/popular/answer/{questionId}
     */
    @PostMapping("/popular/answer/{questionId}")
    public ResponseEntity<Map<String, Object>> onQuestionAnswered(@PathVariable String questionId) {
        redisService.onQuestionAnswered(questionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "回答成功，已更新受欢迎度");
        
        return ResponseEntity.ok(result);
    }

    // ==================== 功能2：最近最常查看问题缓存 ====================

    /**
     * 记录问题查看
     * POST /api/redis/view/{questionId}
     */
    @PostMapping("/view/{questionId}")
    public ResponseEntity<Map<String, Object>> recordQuestionView(@PathVariable String questionId) {
        redisService.recordQuestionView(questionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "查看记录已保存");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取最近最常查看的问题
     * GET /api/redis/view/most?topN=10
     */
    @GetMapping("/view/most")
    public ResponseEntity<Map<String, Object>> getMostViewedQuestions(
            @RequestParam(defaultValue = "10") int topN) {
        
        List<Question> questions = redisService.getMostViewedQuestions(topN);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", questions);
        result.put("count", questions.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 从缓存获取问题详情
     * GET /api/redis/view/detail/{questionId}
     */
    @GetMapping("/view/detail/{questionId}")
    public ResponseEntity<Map<String, Object>> getQuestionFromCache(@PathVariable String questionId) {
        Question question = redisService.getQuestionFromCache(questionId);
        
        Map<String, Object> result = new HashMap<>();
        if (question != null) {
            result.put("success", true);
            result.put("data", question);
            result.put("source", "cache");
        } else {
            result.put("success", false);
            result.put("message", "缓存中未找到该问题");
        }
        
        return ResponseEntity.ok(result);
    }

    /**
     * 使问题缓存失效
     * DELETE /api/redis/view/invalidate/{questionId}
     */
    @DeleteMapping("/view/invalidate/{questionId}")
    public ResponseEntity<Map<String, Object>> invalidateQuestionCache(@PathVariable String questionId) {
        redisService.invalidateQuestionCache(questionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "缓存已失效");
        
        return ResponseEntity.ok(result);
    }

    // ==================== 功能3：热点问题预加载缓存 ====================

    /**
     * 手动触发热点问题预加载
     * POST /api/redis/hot/preload
     */
    @PostMapping("/hot/preload")
    public ResponseEntity<Map<String, Object>> preloadHotQuestions() {
        redisService.preloadHotQuestions();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "热点问题预加载完成");
        
        return ResponseEntity.ok(result);
    }

    /**
     * 获取热点问题列表
     * GET /api/redis/hot/list
     */
    @GetMapping("/hot/list")
    public ResponseEntity<Map<String, Object>> getHotQuestions() {
        List<Question> questions = redisService.getHotQuestions();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", questions);
        result.put("count", questions.size());
        
        return ResponseEntity.ok(result);
    }

    /**
     * 检查问题是否为热点
     * GET /api/redis/hot/check/{questionId}
     */
    @GetMapping("/hot/check/{questionId}")
    public ResponseEntity<Map<String, Object>> isHotQuestion(@PathVariable String questionId) {
        boolean isHot = redisService.isHotQuestion(questionId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("isHot", isHot);
        
        return ResponseEntity.ok(result);
    }

    // ==================== 缓存统计 ====================

    /**
     * 获取缓存统计信息
     * GET /api/redis/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // 热点问题数量
        List<Question> hotQuestions = redisService.getHotQuestions();
        stats.put("hotQuestionsCount", hotQuestions.size());
        
        // 最受欢迎问题数量
        List<Question> popularQuestions = redisService.getPopularQuestions(100);
        stats.put("popularQuestionsCount", popularQuestions.size());
        
        // 最常查看问题数量
        List<Question> viewedQuestions = redisService.getMostViewedQuestions(100);
        stats.put("viewedQuestionsCount", viewedQuestions.size());
        
        stats.put("success", true);
        
        return ResponseEntity.ok(stats);
    }
}
