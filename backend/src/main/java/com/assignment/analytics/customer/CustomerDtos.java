package com.assignment.analytics.customer;

import com.assignment.analytics.domain.ActivityType;
import com.assignment.analytics.domain.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class CustomerDtos {

    private CustomerDtos() {
    }

    public record CustomerSearchItem(UUID customerId, String fullName, String segment, String country,
                                     long transactionCount) {
    }

    public record ActivityTypeSummary(ActivityType type, long count, Map<String, BigDecimal> totalsByCurrency,
                                      long completed, long pending, long failed, long reversed) {
    }

    public record FiredRuleSummary(String ruleName, String appliesTo, long timesFired,
                                   BigDecimal totalContribution) {
    }

    public record CustomerOverview(UUID customerId, String fullName, String email, String segment,
                                   String country, LocalDateTime customerSince, BigDecimal riskScore,
                                   long totalTransactions, LocalDateTime firstActivityAt,
                                   LocalDateTime lastActivityAt, List<ActivityTypeSummary> activity,
                                   List<FiredRuleSummary> firedRules) {
    }

    public record CardDetails(String cardPan, String cardType, String merchantName, String mccCode,
                              boolean cardPresent, String authorizationCode, String declineReason) {
    }

    public record PaymentDetails(String paymentMethod, String senderAccount, String receiverAccount,
                                 String receiverBankCountry) {
    }

    public record CryptoDetails(String blockchain, String walletAddressFrom, String walletAddressTo,
                                String txHash, String exchangeName) {
    }

    public record FiredRule(String ruleName, BigDecimal scoreContribution) {
    }

    public record TransactionItem(UUID transactionId, ActivityType activityType, BigDecimal amount,
                                  String currency, TransactionStatus status, LocalDateTime createdAt,
                                  CardDetails card, PaymentDetails payment, CryptoDetails crypto,
                                  List<FiredRule> firedRules) {
    }

    public record TransactionsPage(List<TransactionItem> items, int page, int size, long totalItems,
                                   int totalPages) {
    }
}
