package com.assignment.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "risk_assessments")
public class RiskAssessment {

    @Id
    @Column(name = "assessment_id")
    private UUID assessmentId;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "rule_id", nullable = false)
    private UUID ruleId;

    @Column(name = "triggered_at", nullable = false)
    private LocalDateTime triggeredAt;

    @Column(name = "score_contribution", nullable = false)
    private BigDecimal scoreContribution;

    protected RiskAssessment() {
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getRuleId() {
        return ruleId;
    }

    public LocalDateTime getTriggeredAt() {
        return triggeredAt;
    }

    public BigDecimal getScoreContribution() {
        return scoreContribution;
    }
}
