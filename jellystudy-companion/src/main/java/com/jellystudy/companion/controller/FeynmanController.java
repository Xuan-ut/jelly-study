package com.jellystudy.companion.controller;

import com.jellystudy.companion.dto.Result;
import com.jellystudy.companion.service.feynman.FeynmanService;
import com.jellystudy.entity.FeynmanResponseDTO;
import com.jellystudy.entity.FeynmanSessionDTO;
import com.jellystudy.entity.UnderstandingAssessmentDTO;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/companion/feynman")
@Slf4j
@RequiredArgsConstructor
public class FeynmanController {

    private final FeynmanService feynmanService;

    @PostMapping("/start")
    public Result<FeynmanSessionDTO> start(@RequestBody StartRequest request) {
        FeynmanService.FeynmanSessionResult result = feynmanService.startSession(
                request.getUserId(), request.getKnowledgeId());
        FeynmanSessionDTO dto = new FeynmanSessionDTO();
        dto.setSessionId(result.getSessionId());
        dto.setUserId(request.getUserId());
        dto.setKnowledgeId(result.getKnowledgeId());
        dto.setKnowledgeName(result.getKnowledgeName());
        dto.setAiQuestion(result.getAiQuestion());
        dto.setStatus(result.getStatus());
        dto.setRoundCount(result.getRoundCount());
        return Result.success(dto);
    }

    @PostMapping("/respond")
    public Result<FeynmanResponseDTO> respond(@RequestBody RespondRequest request) {
        FeynmanService.FeynmanRespondResult result = feynmanService.respond(
                request.getSessionId(), request.getUserExplanation());

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
        return Result.success(dto);
    }

    @GetMapping("/assessment")
    public Result<UnderstandingAssessmentDTO> getAssessment(@RequestParam String sessionId) {
        FeynmanService.AssessmentResult result = feynmanService.getAssessment(sessionId);
        UnderstandingAssessmentDTO dto = new UnderstandingAssessmentDTO();
        dto.setSessionId(result.getSessionId());
        dto.setKnowledgeId(result.getKnowledgeId());
        dto.setOverallScore(result.getOverallScore());
        dto.setMissingPoints(result.getMissingPoints());
        dto.setMisconceptions(result.getMisconceptions());
        dto.setRecommendedReview(result.getRecommendedReview());
        dto.setSuggestedNextStep(result.getSuggestedNextStep());
        return Result.success(dto);
    }

    @Data
    public static class StartRequest {
        private Long userId;
        private String knowledgeId;
    }

    @Data
    public static class RespondRequest {
        private String sessionId;
        private String userExplanation;
    }
}
