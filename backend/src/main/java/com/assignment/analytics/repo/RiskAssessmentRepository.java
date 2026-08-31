package com.assignment.analytics.repo;

import com.assignment.analytics.domain.RiskAssessment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RiskAssessmentRepository extends JpaRepository<RiskAssessment, UUID> {

    List<RiskAssessment> findByTransactionIdIn(Collection<UUID> transactionIds);
}
