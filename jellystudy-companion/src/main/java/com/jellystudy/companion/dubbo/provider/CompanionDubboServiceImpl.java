package com.jellystudy.companion.dubbo.provider;

import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.FeynmanSession;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.SpiritState;
import com.jellystudy.companion.service.feynman.FeynmanService;
import com.jellystudy.companion.service.hive.HiveService;
import com.jellystudy.companion.service.spirit.SpiritService;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritChatResult;
import com.jellystudy.companion.service.spirit.SpiritService.SpiritGreetingResult;
import com.jellystudy.companion.service.timeline.TimelineService;
import com.jellystudy.companion.util.CompanionConverter;
import com.jellystudy.dubbo.CompanionDubboService;
import com.jellystudy.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Companion Dubbo 服务 Provider 实现
 * 所有方法直接委托给对应的 Service，不做业务逻辑
 */
@DubboService(version = "1.0.0", interfaceName = "com.jellystudy.dubbo.CompanionDubboService")
@Slf4j
@RequiredArgsConstructor
public class CompanionDubboServiceImpl implements CompanionDubboService {

    private final SpiritService spiritService;
    private final FeynmanService feynmanService;
    private final TimelineService timelineService;
    private final HiveService hiveService;

    // ===== 精灵系统 =====

    @Override
    public SpiritStateDTO getSpiritState(Long userId) {
        log.info("Dubbo: getSpiritState, userId={}", userId);
        SpiritState state = spiritService.getSpiritState(userId);
        return CompanionConverter.toSpiritStateDTO(state);
    }

    @Override
    public SpiritGreetingDTO getSpiritGreeting(Long userId) {
        log.info("Dubbo: getSpiritGreeting, userId={}", userId);
        SpiritGreetingResult result = spiritService.getGreeting(userId);
        return CompanionConverter.toSpiritGreetingDTO(
                result.getEmotion(), result.getGreeting(), result.getSuggestion());
    }

    @Override
    public SpiritChatResponseDTO chatWithSpirit(Long userId, String sessionId, String message) {
        log.info("Dubbo: chatWithSpirit, userId={}, sessionId={}", userId, sessionId);
        SpiritChatResult result = spiritService.chat(userId, sessionId, message);
        SpiritChatResponseDTO dto = new SpiritChatResponseDTO();
        dto.setSessionId(result.getSessionId());
        dto.setSpiritMessage(result.getSpiritMessage());
        dto.setEmotion(result.getEmotion());
        return dto;
    }

    @Override
    public SpiritStateDTO feedSpirit(Long userId, String eventType, int feedValue) {
        log.info("Dubbo: feedSpirit, userId={}, eventType={}, feedValue={}", userId, eventType, feedValue);
        SpiritState state = spiritService.feed(userId, eventType, feedValue);
        return CompanionConverter.toSpiritStateDTO(state);
    }

    // ===== 费曼反转教学 =====

    @Override
    public FeynmanSessionDTO startFeynmanSession(Long userId, String knowledgeId) {
        log.info("Dubbo: startFeynmanSession, userId={}, knowledgeId={}", userId, knowledgeId);
        FeynmanService.FeynmanSessionResult result = feynmanService.startSession(userId, knowledgeId);
        FeynmanSessionDTO dto = new FeynmanSessionDTO();
        dto.setSessionId(result.getSessionId());
        dto.setUserId(userId);
        dto.setKnowledgeId(result.getKnowledgeId());
        dto.setKnowledgeName(result.getKnowledgeName());
        dto.setAiQuestion(result.getAiQuestion());
        dto.setStatus(result.getStatus());
        dto.setRoundCount(result.getRoundCount());
        return dto;
    }

    @Override
    public FeynmanResponseDTO feynmanRespond(String sessionId, String userExplanation) {
        log.info("Dubbo: feynmanRespond, sessionId={}", sessionId);
        FeynmanService.FeynmanRespondResult result = feynmanService.respond(sessionId, userExplanation);

        FeynmanResponseDTO dto = new FeynmanResponseDTO();
        dto.setSessionId(result.getSessionId());
        dto.setRoundNumber(result.getRoundNumber());
        dto.setAiQuestion(result.getAiQuestion());
        dto.setSpiritReaction(result.getSpiritReaction());
        dto.setSessionComplete(result.isSessionComplete());

        FeynmanResponseDTO.Assessment a = new FeynmanResponseDTO.Assessment();
        a.setAccuracy(result.getAccuracy());
        a.setCompleteness(result.getCompleteness());
        a.setDepth(result.getDepth());
        a.setClarity(result.getClarity());
        a.setAbilityToExample(result.getAbilityToExample());
        a.setOverallScore(result.getOverallScore());
        dto.setAssessment(a);
        return dto;
    }

    @Override
    public UnderstandingAssessmentDTO getUnderstandingAssessment(String sessionId) {
        log.info("Dubbo: getUnderstandingAssessment, sessionId={}", sessionId);
        FeynmanService.AssessmentResult result = feynmanService.getAssessment(sessionId);
        UnderstandingAssessmentDTO dto = new UnderstandingAssessmentDTO();
        dto.setSessionId(result.getSessionId());
        dto.setKnowledgeId(result.getKnowledgeId());
        dto.setOverallScore(result.getOverallScore());
        dto.setMissingPoints(result.getMissingPoints());
        dto.setMisconceptions(result.getMisconceptions());
        dto.setRecommendedReview(result.getRecommendedReview());
        dto.setSuggestedNextStep(result.getSuggestedNextStep());
        return dto;
    }

    // ===== 时空预测 =====

    @Override
    public KnowledgeTreeSnapshotDTO getKnowledgeTree(Long userId) {
        log.info("Dubbo: getKnowledgeTree, userId={}", userId);
        KnowledgeTreeSnapshot snapshot = timelineService.getKnowledgeTree(userId);
        return CompanionConverter.toKnowledgeTreeSnapshotDTO(snapshot);
    }

    @Override
    public KnowledgeTreePredictionDTO predictKnowledgeTree(Long userId, int daysAhead) {
        log.info("Dubbo: predictKnowledgeTree, userId={}, daysAhead={}", userId, daysAhead);
        TimelineService.KnowledgeTreePrediction result = timelineService.predictKnowledgeTree(userId, daysAhead);
        KnowledgeTreePredictionDTO dto = new KnowledgeTreePredictionDTO();
        dto.setUserId(userId);
        dto.setDaysAhead(daysAhead);
        if (result.getCurrent() != null) {
            dto.setCurrentDate(result.getCurrent().getDate());
        }
        // 填充乐观预测
        if (result.getOptimistic() != null) {
            KnowledgeTreePredictionDTO.BranchState opt = new KnowledgeTreePredictionDTO.BranchState();
            if (result.getOptimistic().getPrediction() != null
                    && result.getOptimistic().getPrediction().getOptimistic() != null) {
                KnowledgeTreeSnapshot.Scenario s = result.getOptimistic().getPrediction().getOptimistic();
                opt.setTotalBranches(s.getTotalBranches());
                opt.setAvgMastery(s.getAvgMastery());
                opt.setDescription(s.getDescription());
            }
            dto.setOptimistic(opt);
            dto.setTargetDate(result.getOptimistic().getDate());
        }
        // 填充悲观预测
        if (result.getPessimistic() != null) {
            KnowledgeTreePredictionDTO.BranchState pes = new KnowledgeTreePredictionDTO.BranchState();
            if (result.getPessimistic().getPrediction() != null
                    && result.getPessimistic().getPrediction().getPessimistic() != null) {
                KnowledgeTreeSnapshot.Scenario s = result.getPessimistic().getPrediction().getPessimistic();
                pes.setTotalBranches(s.getTotalBranches());
                pes.setAvgMastery(s.getAvgMastery());
                pes.setDescription(s.getDescription());
            }
            dto.setPessimistic(pes);
        }
        return dto;
    }

    @Override
    public LearningTimelineDTO getLearningTimeline(Long userId) {
        log.info("Dubbo: getLearningTimeline, userId={}", userId);
        return new LearningTimelineDTO();
    }

    @Override
    public List<EarlyWarningDTO> getEarlyWarnings(Long userId) {
        log.info("Dubbo: getEarlyWarnings, userId={}", userId);
        List<TimelineService.EarlyWarning> warnings = timelineService.getEarlyWarnings(userId);
        return warnings.stream().map(w -> {
            EarlyWarningDTO dto = new EarlyWarningDTO();
            dto.setWarningId(w.getWarningId());
            dto.setType(w.getType());
            dto.setSeverity(w.getSeverity());
            dto.setKnowledgePoint(w.getKnowledgePoint());
            dto.setDescription(w.getDescription());
            dto.setAffectedUserRate(w.getAffectedUserRate());
            dto.setRecommendedAction(w.getRecommendedAction());
            return dto;
        }).collect(Collectors.toList());
    }

    // ===== 群体智慧蜂巢 =====

    @Override
    public List<HivePatternDTO> getHivePatterns(String subject) {
        log.info("Dubbo: getHivePatterns, subject={}", subject);
        List<HivePattern> patterns = hiveService.getPatterns(subject);
        return patterns.stream().map(p -> {
            HivePatternDTO dto = new HivePatternDTO();
            dto.setPatternId(p.getPatternId());
            dto.setType(p.getType() != null ? p.getType().name() : null);
            dto.setSubject(p.getSubject());
            dto.setKnowledgeId(p.getKnowledgeId());
            dto.setDescription(p.getDescription());
            dto.setStatistics(p.getStatistics());
            dto.setDiscoveredAt(p.getDiscoveredAt());
            dto.setConfidence(p.getConfidence());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public AnomalyReportDTO getAnomalyReport(Long userId) {
        log.info("Dubbo: getAnomalyReport, userId={}", userId);
        AnomalyRecord record = hiveService.getAnomalyReport(userId);
        return CompanionConverter.toAnomalyReportDTO(record);
    }

    @Override
    public List<LearningPathRecommendationDTO> getRecommendedPaths(Long userId, String subject) {
        log.info("Dubbo: getRecommendedPaths, userId={}, subject={}", userId, subject);
        // 委托给HiveService获取路径推荐
        List<Map<String, Object>> paths = new ArrayList<>(); // TODO: 通过HiveService获取
        return paths.stream().map(p -> {
            LearningPathRecommendationDTO dto = new LearningPathRecommendationDTO();
            dto.setPathId((String) p.get("pathId"));
            dto.setName((String) p.get("name"));
            dto.setSuccessRate(((Number) p.getOrDefault("successRate", 0.0)).doubleValue());
            dto.setAvgDays(((Number) p.getOrDefault("avgDays", 0)).intValue());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public LearningHealthReportDTO getHealthReport(Long userId) {
        log.info("Dubbo: getHealthReport, userId={}", userId);
        LearningHealthReportDTO dto = new LearningHealthReportDTO();
        dto.setUserId(userId);
        dto.setReportDate(java.time.LocalDate.now().toString());
        return dto;
    }
}
