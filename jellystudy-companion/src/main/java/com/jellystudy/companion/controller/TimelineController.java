package com.jellystudy.companion.controller;

import com.jellystudy.companion.dto.Result;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot;
import com.jellystudy.companion.entity.KnowledgeTreeSnapshot.Scenario;
import com.jellystudy.companion.service.timeline.TimelineService;
import com.jellystudy.companion.util.CompanionConverter;
import com.jellystudy.entity.EarlyWarningDTO;
import com.jellystudy.entity.KnowledgeTreeSnapshotDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companion/timeline")
@Slf4j
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/tree")
    public Result<KnowledgeTreeSnapshotDTO> getTree(@RequestParam Long userId) {
        KnowledgeTreeSnapshot snapshot = timelineService.getKnowledgeTree(userId);
        return Result.success(CompanionConverter.toKnowledgeTreeSnapshotDTO(snapshot));
    }

    @GetMapping("/predict")
    public Result<Map<String, Object>> predict(@RequestParam Long userId,
                                                @RequestParam(defaultValue = "90") int daysAhead) {
        TimelineService.KnowledgeTreePrediction result = timelineService.predictKnowledgeTree(userId, daysAhead);
        Map<String, Object> data = new HashMap<>();

        if (result.getOptimistic() != null && result.getOptimistic().getPrediction() != null
                && result.getOptimistic().getPrediction().getOptimistic() != null) {
            Scenario s = result.getOptimistic().getPrediction().getOptimistic();
            Map<String, Object> opt = new HashMap<>();
            opt.put("totalBranches", s.getTotalBranches());
            opt.put("avgMastery", s.getAvgMastery());
            opt.put("description", s.getDescription() != null ? s.getDescription() : "保持当前学习节奏");
            opt.put("date", result.getOptimistic().getDate() != null ? result.getOptimistic().getDate().toString() : "");
            data.put("optimistic", opt);
        }
        if (result.getPessimistic() != null && result.getPessimistic().getPrediction() != null
                && result.getPessimistic().getPrediction().getPessimistic() != null) {
            Scenario s = result.getPessimistic().getPrediction().getPessimistic();
            Map<String, Object> pes = new HashMap<>();
            pes.put("totalBranches", s.getTotalBranches());
            pes.put("avgMastery", s.getAvgMastery());
            pes.put("description", s.getDescription() != null ? s.getDescription() : "停止学习后的预测");
            pes.put("date", result.getPessimistic().getDate() != null ? result.getPessimistic().getDate().toString() : "");
            data.put("pessimistic", pes);
        }
        return Result.success(data);
    }

    @GetMapping("/history")
    public Result<String> getHistory(@RequestParam Long userId) {
        timelineService.getLearningTimeline(userId);
        return Result.success("时间线获取完成");
    }

    @GetMapping("/warnings")
    public Result<List<EarlyWarningDTO>> getWarnings(@RequestParam Long userId) {
        List<TimelineService.EarlyWarning> warnings = timelineService.getEarlyWarnings(userId);
        List<EarlyWarningDTO> dtos = warnings.stream().map(w -> {
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
        return Result.success(dtos);
    }
}
