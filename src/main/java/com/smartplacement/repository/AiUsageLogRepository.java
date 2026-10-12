package com.smartplacement.repository;

import com.smartplacement.entity.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    List<AiUsageLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<AiUsageLog> findByFeatureName(String featureName);
}
