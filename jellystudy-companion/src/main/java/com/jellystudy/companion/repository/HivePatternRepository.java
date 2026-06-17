package com.jellystudy.companion.repository;

import com.jellystudy.companion.entity.HivePattern;
import com.jellystudy.companion.enums.HivePatternType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HivePatternRepository extends MongoRepository<HivePattern, String> {

    List<HivePattern> findByType(HivePatternType type);

    List<HivePattern> findBySubject(String subject);

    Optional<HivePattern> findByPatternId(String patternId);

    List<HivePattern> findByConfidenceGreaterThanEqual(double confidence);
}
