package com.jellystudy.repository.mysql;

import com.jellystudy.entity.JpaUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<JpaUser, Long> {
    Optional<JpaUser> findByUsername(String username);
    boolean existsByUsername(String username);
}
