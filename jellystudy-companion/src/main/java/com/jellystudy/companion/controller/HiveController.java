package com.jellystudy.companion.controller;

import com.jellystudy.companion.dto.Result;
import com.jellystudy.companion.entity.AnomalyRecord;
import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.service.hive.HiveService;
import com.jellystudy.companion.util.CompanionConverter;
import com.jellystudy.entity.AnomalyReportDTO;
import com.jellystudy.entity.HivePatternDTO;
import com.jellystudy.entity.LearningHealthReportDTO;
import com.jellystudy.entity.LearningPathRecommendationDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/companion/hive")
@Slf4j
@RequiredArgsConstructor
public class HiveController {

    private final HiveService hiveService;

    @GetMapping("/patterns")
    public Result<List<HivePatternDTO>> getPatterns(@RequestParam(defaultValue = "Java") String subject) {
        List<HivePattern> patterns = hiveService.getPatterns(subject);
        List<HivePatternDTO> dtos = patterns.stream().map(p -> {
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
        return Result.success(dtos);
    }

    @GetMapping("/anomaly")
    public Result<AnomalyReportDTO> getAnomalyReport(@RequestParam Long userId) {
        AnomalyRecord record = hiveService.getAnomalyReport(userId);
        return Result.success(CompanionConverter.toAnomalyReportDTO(record));
    }

    @GetMapping("/paths")
    public Result<List<LearningPathRecommendationDTO>> getPaths(@RequestParam Long userId,
                                                                  @RequestParam(defaultValue = "Java") String subject) {
        return Result.success(Collections.emptyList());
    }

    @GetMapping("/health")
    public Result<LearningHealthReportDTO> getHealthReport(@RequestParam Long userId) {
        LearningHealthReportDTO dto = new LearningHealthReportDTO();
        dto.setUserId(userId);
        dto.setReportDate(java.time.LocalDate.now().toString());
        return Result.success(dto);
    }
}
