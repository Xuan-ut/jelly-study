package com.jellystudy.companion.repository;

import com.jellystudy.companion.entity.AnomalyRecord;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnomalyRecordRepository extends MongoRepository<AnomalyRecord, String> {

    Optional<AnomalyRecord> findTopByUserIdOrderByReportDateDesc(Long userId);

    List<AnomalyRecord> findByUserIdOrderByReportDateDesc(Long userId);
}
