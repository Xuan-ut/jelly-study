package com.jellystudy.repository.mongo;

import com.jellystudy.entity.StudyPlan;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudyPlanRepository extends MongoRepository<StudyPlan, String> {
    List<StudyPlan> findByUserId(Long userId);
    List<StudyPlan> findByUserIdAndStatus(Long userId, String status);
}
