package com.jellystudy.service;

import com.jellystudy.dubbo.QuestionDubboService;
import com.jellystudy.entity.Question;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis服务类 - 实现三个核心功能：
 * 1. 最近最受欢迎问题排行榜
 * 2. 最近最常查看问题缓存
 * 3. 热点问题预加载缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    @DubboReference(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.QuestionDubboService", timeout = 120000, check = false)
    private QuestionDubboService questionService;

    // ==================== 功能1：最近最受欢迎问题排行榜 ====================

    /**
     * 最受欢迎问题定义：
     * - 综合得分 = 点赞数 × 1 + 回答数 × 2 + 评论数 × 0.5
     * - 时间衰减因子：最近7天权重较高，超过7天逐渐衰减
     * 
     * 缓存设计：
     * - 数据结构：Sorted Set (ZSET)
     * - Key: "question:popular:rank"
     * - Value: 问题ID
     * - Score: 综合得分（带时间衰减）
     * 
     * 更新策略：
     * - 实时更新：点赞、回答、评论时更新分数
     * - 定时更新：每天凌晨重新计算所有问题得分
     * - 时间窗口：只保留最近30天的数据
     */

    private static final String POPULAR_RANK_KEY = "question:popular:rank";
    private static final int POPULAR_TIME_WINDOW_DAYS = 30;

    /**
     * 更新问题的受欢迎度分数
     */
    public void updatePopularScore(String questionId, int likeCount, int answerCount, int commentCount, Date createTime) {
        double score = calculatePopularScore(likeCount, answerCount, commentCount, createTime);
        redisTemplate.opsForZSet().add(POPULAR_RANK_KEY, questionId, score);
        
        // 设置过期时间（30天）
        redisTemplate.expire(POPULAR_RANK_KEY, POPULAR_TIME_WINDOW_DAYS, TimeUnit.DAYS);
        
        log.debug("更新问题[{}]的受欢迎度分数: {}", questionId, score);
    }

    /**
     * 计算受欢迎度分数
     */
    private double calculatePopularScore(int likeCount, int answerCount, int commentCount, Date createTime) {
        // 基础得分
        double baseScore = likeCount * 1.0 + answerCount * 2.0 + commentCount * 0.5;
        
        // 时间衰减因子（最近7天权重为1，超过7天每天衰减10%）
        long daysSinceCreated = (System.currentTimeMillis() - createTime.getTime()) / (1000 * 60 * 60 * 24);
        double decayFactor = daysSinceCreated <= 7 ? 1.0 : Math.pow(0.9, daysSinceCreated - 7);
        
        return baseScore * decayFactor;
    }

    /**
     * 获取最受欢迎问题排行榜
     * @param topN 返回前N个问题
     */
    public List<Question> getPopularQuestions(int topN) {
        Set<Object> questionIds = redisTemplate.opsForZSet()
                .reverseRange(POPULAR_RANK_KEY, 0, topN - 1);
        
        if (questionIds == null || questionIds.isEmpty()) {
            // 如果缓存为空，从数据库加载并预热
            return loadAndWarmPopularCache(topN);
        }
        
        List<Question> questions = new ArrayList<>();
        for (Object id : questionIds) {
            Question question = questionService.findById((String) id);
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    /**
     * 加载并预热热门问题缓存
     */
    private List<Question> loadAndWarmPopularCache(int topN) {
        List<Question> allQuestions = questionService.findAll();
        
        // 计算每个问题的分数并更新缓存
        for (Question q : allQuestions) {
            int commentCount = q.getAnswers().stream()
                    .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                    .sum();
            updatePopularScore(q.getId(), q.getLikeCount(), 
                    q.getAnswers() != null ? q.getAnswers().size() : 0, 
                    commentCount, q.getCreateTime());
        }
        
        // 返回前N个
        return getPopularQuestions(topN);
    }

    /**
     * 点赞时更新受欢迎度
     */
    public void onQuestionLiked(String questionId) {
        Question question = questionService.findById(questionId);
        if (question != null) {
            int commentCount = question.getAnswers().stream()
                    .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                    .sum();
            updatePopularScore(questionId, question.getLikeCount() + 1,
                    question.getAnswers() != null ? question.getAnswers().size() : 0,
                    commentCount, question.getCreateTime());
        }
    }

    /**
     * 回答时更新受欢迎度
     */
    public void onQuestionAnswered(String questionId) {
        Question question = questionService.findById(questionId);
        if (question != null) {
            int answerCount = question.getAnswers() != null ? question.getAnswers().size() : 0;
            int commentCount = question.getAnswers().stream()
                    .mapToInt(a -> a.getComments() != null ? a.getComments().size() : 0)
                    .sum();
            updatePopularScore(questionId, question.getLikeCount(), answerCount + 1,
                    commentCount, question.getCreateTime());
        }
    }

    // ==================== 功能2：最近最常查看问题缓存 ====================

    /**
     * 最热问题定义：
     * - 最近被查看次数最多的问题
     * - 基于滑动窗口统计（最近24小时）
     * 
     * 缓存设计：
     * - 数据结构：Sorted Set (ZSET) + Hash
     * - ZSET Key: "question:view:rank" - 存储问题ID和查看次数
     * - Hash Key: "question:view:detail:{id}" - 存储问题详情
     * 
     * 更新策略：
     * - 实时更新：每次查看问题时增加计数
     * - 定时清理：每小时清理超过24小时的数据
     * 
     * 缓存/数据同步策略：
     * - 写回策略：缓存更新后，定期同步到数据库
     * - 失效策略：问题更新时，删除对应的缓存详情
     * - 预热策略：系统启动时加载最近热门问题
     */

    private static final String VIEW_RANK_KEY = "question:view:rank";
    private static final String VIEW_DETAIL_PREFIX = "question:view:detail:";
    private static final int VIEW_TIME_WINDOW_HOURS = 24;

    /**
     * 记录问题查看
     */
    public void recordQuestionView(String questionId) {
        // 增加查看计数
        redisTemplate.opsForZSet().incrementScore(VIEW_RANK_KEY, questionId, 1);
        
        // 设置过期时间（24小时）
        redisTemplate.expire(VIEW_RANK_KEY, VIEW_TIME_WINDOW_HOURS, TimeUnit.HOURS);
        
        // 更新问题详情缓存
        Question question = questionService.findById(questionId);
        if (question != null) {
            String detailKey = VIEW_DETAIL_PREFIX + questionId;
            redisTemplate.opsForHash().putAll(detailKey, questionToMap(question));
            redisTemplate.expire(detailKey, VIEW_TIME_WINDOW_HOURS, TimeUnit.HOURS);
        }
        
        log.debug("记录问题[{}]查看", questionId);
    }

    /**
     * 获取最近最常查看的问题
     */
    public List<Question> getMostViewedQuestions(int topN) {
        Set<Object> questionIds = redisTemplate.opsForZSet()
                .reverseRange(VIEW_RANK_KEY, 0, topN - 1);
        
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<Question> questions = new ArrayList<>();
        for (Object id : questionIds) {
            Question question = getQuestionFromCache((String) id);
            if (question == null) {
                question = questionService.findById((String) id);
            }
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    /**
     * 从缓存获取问题详情
     */
    public Question getQuestionFromCache(String questionId) {
        String detailKey = VIEW_DETAIL_PREFIX + questionId;
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(detailKey);
        
        if (hash.isEmpty()) {
            return null;
        }
        
        return mapToQuestion(hash);
    }

    /**
     * 当问题更新时，使缓存失效
     */
    public void invalidateQuestionCache(String questionId) {
        String detailKey = VIEW_DETAIL_PREFIX + questionId;
        redisTemplate.delete(detailKey);
        log.debug("使问题[{}]缓存失效", questionId);
    }

    // ==================== 功能3：热点问题预加载缓存（自行设计场景） ====================

    /**
     * 场景设计：热点问题预加载
     * 
     * 业务需求：
     * - 在流量高峰期（如早上9点、晚上8点），热门问题访问量激增
     * - 提前预加载热点问题到缓存，避免数据库压力
     * 
     * 缓存设计：
     * - 数据结构：List + Hash
     * - List Key: "question:hot:list" - 存储热点问题ID列表
     * - Hash Key: "question:hot:detail:{id}" - 存储完整问题详情
     * 
     * 更新策略：
     * - 定时任务：每天8:30、19:30预加载当天热点问题
     * - 事件触发：当某个问题短时间内访问量激增时自动加入热点列表
     * - 淘汰策略：LRU淘汰，保留Top 50热点问题
     * 
     * 使用场景：
     * - 首页热榜展示
     * - 推荐系统数据源
     * - 减轻数据库读压力
     */

    private static final String HOT_LIST_KEY = "question:hot:list";
    private static final String HOT_DETAIL_PREFIX = "question:hot:detail:";
    private static final int HOT_LIST_MAX_SIZE = 50;

    /**
     * 预加载热点问题
     */
    public void preloadHotQuestions() {
        // 获取当前热点问题
        List<Question> popularQuestions = getPopularQuestions(HOT_LIST_MAX_SIZE);
        
        // 清除旧的热点列表
        redisTemplate.delete(HOT_LIST_KEY);
        
        // 重新设置热点列表和详情
        for (Question question : popularQuestions) {
            redisTemplate.opsForList().rightPush(HOT_LIST_KEY, question.getId());
            
            String detailKey = HOT_DETAIL_PREFIX + question.getId();
            redisTemplate.opsForHash().putAll(detailKey, questionToMap(question));
            redisTemplate.expire(detailKey, 2, TimeUnit.HOURS);
        }
        
        redisTemplate.expire(HOT_LIST_KEY, 2, TimeUnit.HOURS);
        log.info("预加载热点问题完成，共{}个", popularQuestions.size());
    }

    /**
     * 获取热点问题列表
     */
    public List<Question> getHotQuestions() {
        List<Object> questionIds = redisTemplate.opsForList().range(HOT_LIST_KEY, 0, -1);
        
        if (questionIds == null || questionIds.isEmpty()) {
            // 如果缓存为空，触发预加载
            preloadHotQuestions();
            return getHotQuestions();
        }
        
        List<Question> questions = new ArrayList<>();
        for (Object id : questionIds) {
            Question question = getHotQuestionFromCache((String) id);
            if (question == null) {
                question = questionService.findById((String) id);
            }
            if (question != null) {
                questions.add(question);
            }
        }
        return questions;
    }

    /**
     * 从热点缓存获取问题详情
     */
    public Question getHotQuestionFromCache(String questionId) {
        String detailKey = HOT_DETAIL_PREFIX + questionId;
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(detailKey);
        
        if (hash.isEmpty()) {
            return null;
        }
        
        return mapToQuestion(hash);
    }

    /**
     * 检查问题是否为热点
     */
    public boolean isHotQuestion(String questionId) {
        return redisTemplate.opsForList().range(HOT_LIST_KEY, 0, -1)
                .stream().anyMatch(id -> id.equals(questionId));
    }

    // ==================== 新增方法：综合点击量和点赞量的热点问题获取 ====================

    /**
     * 获取热点问题（主要基于点击量，结合点赞量）
     * 
     * 算法设计：
     * - 主要参考：Redis中的点击量（占70%权重）
     * - 次要参考：点赞量（占30%权重）
     * - 综合得分 = 点击量 × 0.7 + 点赞量 × 0.3
     * 
     * @param topN 返回前N个问题
     */
    public List<Question> getHotQuestionsWithViewAndLike(int topN) {
        // 获取点击量排行榜
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> viewRank = 
                redisTemplate.opsForZSet().reverseRangeWithScores(VIEW_RANK_KEY, 0, -1);
        
        // 获取受欢迎度排行榜（包含点赞量等因素）
        Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object>> popularRank = 
                redisTemplate.opsForZSet().reverseRangeWithScores(POPULAR_RANK_KEY, 0, -1);
        
        // 构建综合得分Map
        Map<String, Double> combinedScores = new HashMap<>();
        
        // 处理点击量数据（权重70%）
        if (viewRank != null && !viewRank.isEmpty()) {
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : viewRank) {
                String questionId = (String) tuple.getValue();
                double viewScore = tuple.getScore() != null ? tuple.getScore() : 0;
                combinedScores.put(questionId, viewScore * 0.7);
            }
        }
        
        // 处理受欢迎度数据（权重30%，主要包含点赞量因素）
        if (popularRank != null && !popularRank.isEmpty()) {
            for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<Object> tuple : popularRank) {
                String questionId = (String) tuple.getValue();
                double popularScore = tuple.getScore() != null ? tuple.getScore() : 0;
                
                // 点赞量在popularScore中已有体现，这里使用归一化后的分数
                double normalizedPopularScore = Math.min(popularScore / 100, 10); // 归一化到0-10
                
                combinedScores.merge(questionId, normalizedPopularScore * 0.3, Double::sum);
            }
        }
        
        // 如果Redis中没有数据，从数据库加载
        if (combinedScores.isEmpty()) {
            return loadAndWarmPopularCache(topN);
        }
        
        // 按综合得分排序
        List<Map.Entry<String, Double>> sortedEntries = new ArrayList<>(combinedScores.entrySet());
        sortedEntries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        // 获取前topN个问题
        List<Question> questions = new ArrayList<>();
        int count = 0;
        for (Map.Entry<String, Double> entry : sortedEntries) {
            if (count >= topN) break;
            
            Question question = getQuestionFromCache(entry.getKey());
            if (question == null) {
                question = questionService.findById(entry.getKey());
            }
            if (question != null) {
                questions.add(question);
                count++;
            }
        }
        
        log.debug("获取热点问题成功（基于点击量+点赞量），共{}个", questions.size());
        return questions;
    }

    // ==================== 功能4：用户浏览历史记录（自行设计Redis场景） ====================

    /**
     * 场景设计：用户浏览历史记录
     *
     * 业务需求：
     * - 记录用户最近浏览过的问题，方便用户回看
     * - 基于会话（sessionId）区分不同用户的浏览记录
     * - 浏览历史按时间倒序排列，最近浏览的排在最前面
     * - 同一问题重复浏览不重复记录，而是移到最前面
     *
     * 缓存设计：
     * - 数据结构：List + Hash
     * - List Key: "question:history:{sessionId}" - 存储问题ID列表（按浏览时间倒序）
     * - Hash Key: "question:history:summary:{id}" - 存储问题摘要（标题、作者等）
     *
     * 更新策略：
     * - 实时更新：每次查看问题时，将问题ID添加到List头部
     * - 去重策略：先移除已存在的相同ID，再添加到头部
     * - 容量限制：最多保留最近20条浏览记录
     * - 过期策略：7天自动过期
     */

    private static final String HISTORY_LIST_PREFIX = "question:history:";
    private static final String HISTORY_SUMMARY_PREFIX = "question:history:summary:";
    private static final int HISTORY_MAX_SIZE = 20;
    private static final int HISTORY_EXPIRE_DAYS = 7;

    /**
     * 记录用户浏览历史
     */
    public void recordBrowseHistory(String sessionId, String questionId) {
        String listKey = HISTORY_LIST_PREFIX + sessionId;

        redisTemplate.opsForList().remove(listKey, 0, questionId);

        redisTemplate.opsForList().leftPush(listKey, questionId);

        redisTemplate.opsForList().trim(listKey, 0, HISTORY_MAX_SIZE - 1);

        redisTemplate.expire(listKey, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);

        Question question = questionService.findById(questionId);
        if (question != null) {
            String summaryKey = HISTORY_SUMMARY_PREFIX + questionId;
            Map<String, Object> summary = new HashMap<>();
            summary.put("id", question.getId());
            summary.put("title", question.getTitle());
            summary.put("author", question.getAuthor());
            summary.put("difficulty", question.getDifficulty());
            summary.put("likeCount", question.getLikeCount());
            if (question.getCreateTime() != null) {
                summary.put("createTime", question.getCreateTime().getTime());
            }
            redisTemplate.opsForHash().putAll(summaryKey, summary);
            redisTemplate.expire(summaryKey, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
        }

        log.debug("记录用户[{}]浏览历史: 问题[{}]", sessionId, questionId);
    }

    /**
     * 获取用户浏览历史
     */
    public List<Map<String, Object>> getBrowseHistory(String sessionId) {
        String listKey = HISTORY_LIST_PREFIX + sessionId;
        List<Object> questionIds = redisTemplate.opsForList().range(listKey, 0, -1);

        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Map<String, Object>> history = new ArrayList<>();
        for (Object id : questionIds) {
            String summaryKey = HISTORY_SUMMARY_PREFIX + id;
            Map<Object, Object> summary = redisTemplate.opsForHash().entries(summaryKey);

            if (summary.isEmpty()) {
                Question question = questionService.findById((String) id);
                if (question != null) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", question.getId());
                    item.put("title", question.getTitle());
                    item.put("author", question.getAuthor());
                    item.put("difficulty", question.getDifficulty());
                    item.put("likeCount", question.getLikeCount());
                    if (question.getCreateTime() != null) {
                        item.put("createTime", question.getCreateTime().getTime());
                    }
                    redisTemplate.opsForHash().putAll(summaryKey, item);
                    redisTemplate.expire(summaryKey, HISTORY_EXPIRE_DAYS, TimeUnit.DAYS);
                    history.add(item);
                }
            } else {
                Map<String, Object> item = new HashMap<>();
                summary.forEach((k, v) -> item.put((String) k, v));
                history.add(item);
            }
        }

        return history;
    }

    /**
     * 清除用户浏览历史
     */
    public void clearBrowseHistory(String sessionId) {
        String listKey = HISTORY_LIST_PREFIX + sessionId;
        List<Object> questionIds = redisTemplate.opsForList().range(listKey, 0, -1);

        if (questionIds != null) {
            for (Object id : questionIds) {
                redisTemplate.delete(HISTORY_SUMMARY_PREFIX + id);
            }
        }

        redisTemplate.delete(listKey);
        log.debug("清除用户[{}]浏览历史", sessionId);
    }

    // ==================== 工具方法 ====================

    /**
     * Question转Map
     */
    private Map<String, Object> questionToMap(Question question) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", question.getId());
        map.put("title", question.getTitle());
        map.put("content", question.getContent());
        map.put("author", question.getAuthor());
        map.put("difficulty", question.getDifficulty());
        map.put("knowledgePoints", question.getKnowledgePoints());
        map.put("likeCount", question.getLikeCount());
        map.put("createTime", question.getCreateTime().getTime());
        return map;
    }

    /**
     * Map转Question
     */
    private Question mapToQuestion(Map<Object, Object> hash) {
        Question question = new Question();
        question.setId((String) hash.get("id"));
        question.setTitle((String) hash.get("title"));
        question.setContent((String) hash.get("content"));
        question.setAuthor((String) hash.get("author"));
        question.setDifficulty((String) hash.get("difficulty"));
        question.setKnowledgePoints((List<String>) hash.get("knowledgePoints"));
        question.setLikeCount((Integer) hash.get("likeCount"));
        
        if (hash.get("createTime") != null) {
            question.setCreateTime(new Date((Long) hash.get("createTime")));
        }
        
        return question;
    }
}
