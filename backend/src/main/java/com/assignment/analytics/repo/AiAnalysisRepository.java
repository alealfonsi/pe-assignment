package com.assignment.analytics.repo;

import com.assignment.analytics.domain.AiAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, UUID> {

    List<AiAnalysis> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);
}
