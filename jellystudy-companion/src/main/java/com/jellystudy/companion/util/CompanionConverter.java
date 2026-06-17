package com.jellystudy.companion.util;

import com.jellystudy.companion.entity.*;
import com.jellystudy.entity.*;

import java.util.stream.Collectors;

/**
 * 实体与DTO互转工具
 */
public final class CompanionConverter {

    private CompanionConverter() {}

    // ===== SpiritState =====
    public static SpiritStateDTO toSpiritStateDTO(SpiritState s) {
        if (s == null) return null;
        SpiritStateDTO dto = new SpiritStateDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setName(s.getName());
        dto.setLevel(s.getLevel() != null ? s.getLevel() : 0);
        dto.setLevelName(s.getLevelName());
        dto.setExperience(s.getExperience() != null ? s.getExperience() : 0);
        dto.setNextLevelExp(s.getNextLevelExp() != null ? s.getNextLevelExp() : 100);
        dto.setSatiation(s.getSatiation() != null ? s.getSatiation() : 100);
        dto.setEmotion(s.getEmotion() != null ? s.getEmotion().name() : null);
        dto.setPersonalityKey(s.getPersonalityKey());
        dto.setAppearanceLevel(s.getAppearanceLevel() != null ? s.getAppearanceLevel() : 0);
        if (s.getAppearance() != null) {
            SpiritStateDTO.Appearance a = new SpiritStateDTO.Appearance();
            a.setBody(s.getAppearance().getBody());
            a.setWings(s.getAppearance().getWings());
            a.setAura(s.getAppearance().getAura());
            a.setCrown(s.getAppearance().getCrown());
            dto.setAppearance(a);
        }
        dto.setSkills(s.getSkills());
        if (s.getMemory() != null) {
            SpiritStateDTO.Memory m = new SpiritStateDTO.Memory();
            m.setLastInteraction(s.getMemory().getLastInteraction());
            m.setRecentTopics(s.getMemory().getRecentTopics());
            m.setUserPreferences(s.getMemory().getUserPreferences());
            dto.setMemory(m);
        }
        if (s.getGrowthLog() != null) {
            dto.setGrowthLog(s.getGrowthLog().stream().map(g -> {
                SpiritStateDTO.GrowthLog gl = new SpiritStateDTO.GrowthLog();
                gl.setDate(g.getDate() != null ? g.getDate().toString() : null);
                gl.setEvent(g.getEvent() != null ? g.getEvent().name() : null);
                gl.setDetail(g.getDetail());
                gl.setExpGained(g.getExpGained() != null ? g.getExpGained() : 0);
                return gl;
            }).collect(Collectors.toList()));
        }
        dto.setCreateTime(s.getCreateTime());
        dto.setUpdateTime(s.getUpdateTime());
        return dto;
    }

    // ===== SpiritGreeting =====
    public static SpiritGreetingDTO toSpiritGreetingDTO(String emotion, String greeting, String suggestion) {
        return new SpiritGreetingDTO(emotion, greeting, suggestion);
    }

    // ===== FeynmanSession =====
    public static FeynmanSessionDTO toFeynmanSessionDTO(FeynmanSession s) {
        if (s == null) return null;
        FeynmanSessionDTO dto = new FeynmanSessionDTO();
        dto.setSessionId(s.getSessionId());
        dto.setUserId(s.getUserId());
        dto.setKnowledgeId(s.getKnowledgeId());
        dto.setKnowledgeName(s.getKnowledgeName());
        dto.setStatus(s.getStatus());
        dto.setRoundCount(s.getRounds() != null ? s.getRounds().size() : 0);
        dto.setStartTime(s.getCreateTime());
        if (!s.getRounds().isEmpty()) {
            dto.setAiQuestion(s.getRounds().get(s.getRounds().size() - 1).getAiQuestion());
        }
        return dto;
    }

    // ===== FeynmanResponse =====
    public static FeynmanResponseDTO toFeynmanResponseDTO(FeynmanSession s, int roundNum,
                                                           String aiQuestion, String spiritReaction, boolean complete) {
        FeynmanResponseDTO dto = new FeynmanResponseDTO();
        dto.setSessionId(s.getSessionId());
        dto.setRoundNumber(roundNum);
        dto.setAiQuestion(aiQuestion);
        dto.setSpiritReaction(spiritReaction);
        dto.setSessionComplete(complete);
        if (!s.getRounds().isEmpty()) {
            FeynmanSession.Round lastRound = s.getRounds().get(s.getRounds().size() - 1);
            FeynmanResponseDTO.Assessment a = new FeynmanResponseDTO.Assessment();
            a.setAccuracy(lastRound.getAssessment().getAccuracy());
            a.setCompleteness(lastRound.getAssessment().getCompleteness());
            a.setDepth(lastRound.getAssessment().getDepth());
            a.setClarity(lastRound.getAssessment().getClarity());
            a.setAbilityToExample(lastRound.getAssessment().getAbilityToExample());
            dto.setAssessment(a);
        }
        return dto;
    }

    // ===== UnderstandingAssessment =====
    public static UnderstandingAssessmentDTO toUnderstandingAssessmentDTO(FeynmanSession s) {
        if (s == null) return null;
        UnderstandingAssessmentDTO dto = new UnderstandingAssessmentDTO();
        dto.setSessionId(s.getSessionId());
        dto.setKnowledgeId(s.getKnowledgeId());
        if (s.getFinalAssessment() != null) {
            dto.setOverallScore(s.getFinalAssessment().getOverallScore());
            dto.setMissingPoints(s.getFinalAssessment().getMissingPoints());
            dto.setMisconceptions(s.getFinalAssessment().getMisconceptions());
            dto.setRecommendedReview(s.getFinalAssessment().getRecommendedReview());
            dto.setSuggestedNextStep(s.getFinalAssessment().getSuggestedNextStep());
        }
        return dto;
    }

    // ===== KnowledgeTreeSnapshot =====
    public static KnowledgeTreeSnapshotDTO toKnowledgeTreeSnapshotDTO(KnowledgeTreeSnapshot s) {
        if (s == null) return null;
        KnowledgeTreeSnapshotDTO dto = new KnowledgeTreeSnapshotDTO();
        dto.setId(s.getId());
        dto.setUserId(s.getUserId());
        dto.setDate(s.getDate());
        if (s.getBranches() != null) {
            dto.setBranches(s.getBranches().stream().map(b -> {
                KnowledgeTreeSnapshotDTO.Branch branch = new KnowledgeTreeSnapshotDTO.Branch();
                branch.setName(b.getName());
                branch.setPlanId(b.getPlanId());
                branch.setPlanStatus(b.getPlanStatus());
                branch.setDescription(b.getDescription());
                branch.setProgress(b.getProgress());
                branch.setStability(b.getStability());
                branch.setPredictedRetention30d(b.getPredictedRetention30d());
                if (b.getNodes() != null) {
                    branch.setNodes(b.getNodes().stream().map(CompanionConverter::toNodeDTO).collect(Collectors.toList()));
                }
                return branch;
            }).collect(Collectors.toList()));
        }
        if (s.getPrediction() != null) {
            KnowledgeTreeSnapshotDTO.Prediction p = new KnowledgeTreeSnapshotDTO.Prediction();
            p.setOptimistic(toScenarioDTO(s.getPrediction().getOptimistic()));
            p.setPessimistic(toScenarioDTO(s.getPrediction().getPessimistic()));
            dto.setPrediction(p);
        }
        return dto;
    }

    /** 递归转换 Node → NodeDTO */
    private static KnowledgeTreeSnapshotDTO.Node toNodeDTO(KnowledgeTreeSnapshot.Node n) {
        if (n == null) return null;
        KnowledgeTreeSnapshotDTO.Node dto = new KnowledgeTreeSnapshotDTO.Node();
        dto.setName(n.getName());
        dto.setMastery(n.getMastery());
        dto.setLastReview(n.getLastReview());
        dto.setType(n.getType());
        dto.setNodeId(n.getNodeId());
        dto.setStatus(n.getStatus());
        if (n.getChildren() != null) {
            dto.setChildren(n.getChildren().stream().map(CompanionConverter::toNodeDTO).collect(Collectors.toList()));
        }
        return dto;
    }

    private static KnowledgeTreeSnapshotDTO.Scenario toScenarioDTO(KnowledgeTreeSnapshot.Scenario s) {
        if (s == null) return null;
        KnowledgeTreeSnapshotDTO.Scenario sc = new KnowledgeTreeSnapshotDTO.Scenario();
        sc.setDate(s.getDate());
        sc.setTotalBranches(s.getTotalBranches());
        sc.setAvgMastery(s.getAvgMastery());
        sc.setDescription(s.getDescription());
        return sc;
    }

    // ===== AnomalyReport =====
    public static AnomalyReportDTO toAnomalyReportDTO(AnomalyRecord r) {
        if (r == null) return null;
        AnomalyReportDTO dto = new AnomalyReportDTO();
        dto.setUserId(r.getUserId());
        dto.setReportDate(r.getReportDate() != null ? r.getReportDate().toString() : null);
        dto.setOverallRisk(r.getOverallRisk());
        dto.setOverallRiskLevel(r.getOverallRiskLevel());
        if (r.getDimensions() != null) {
            dto.setDimensions(r.getDimensions().stream().map(d -> {
                AnomalyReportDTO.AnomalyDimension ad = new AnomalyReportDTO.AnomalyDimension();
                ad.setName(d.getName());
                ad.setUserValue(d.getUserValue());
                ad.setNormalRange(d.getNormalRange());
                ad.setStatus(d.getStatus());
                ad.setRecommendation(d.getRecommendation());
                return ad;
            }).collect(Collectors.toList()));
        }
        return dto;
    }
}
