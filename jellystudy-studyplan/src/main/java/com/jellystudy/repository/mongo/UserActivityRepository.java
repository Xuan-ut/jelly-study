package com.jellystudy.repository.mongo;

import com.jellystudy.entity.UserActivity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserActivityRepository extends MongoRepository<UserActivity, String> {
    List<UserActivity> findByUserIdOrderByCreateTimeDesc(Long userId);
    List<UserActivity> findByUserIdAndActivityTypeOrderByCreateTimeDesc(Long userId, String activityType);
    long countByUserId(Long userId);
    long countByUserIdAndActivityType(Long userId, String activityType);
    List<UserActivity> findTop10ByUserIdOrderByCreateTimeDesc(Long userId);
    List<UserActivity> findTopByUserIdOrderByCreateTimeDesc(Long userId);
}
