package com.assignment.analytics.repo;

import com.assignment.analytics.domain.RiskRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RiskRuleRepository extends JpaRepository<RiskRule, UUID> {
}
