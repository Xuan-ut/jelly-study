package com.jellystudy.repository.mongo;

import com.jellystudy.entity.StudyProgress;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudyProgressRepository extends MongoRepository<StudyProgress, String> {
    Optional<StudyProgress> findByUserIdAndKnowledgePointId(Long userId, String knowledgePointId);
    List<StudyProgress> findByUserId(Long userId);
    List<StudyProgress> findByPlanId(String planId);
    List<StudyProgress> findByUserIdAndPlanId(Long userId, String planId);
    Optional<StudyProgress> findByUserIdAndStageId(Long userId, String stageId);
    List<StudyProgress> findByUserIdAndLastStudyTimeBetween(Long userId, java.util.Date start, java.util.Date end);
    List<StudyProgress> findByUserIdOrderByLastStudyTimeDesc(Long userId);
}
