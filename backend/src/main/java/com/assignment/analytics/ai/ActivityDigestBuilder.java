package com.assignment.analytics.ai;

import com.assignment.analytics.ai.ActivityDigest.NotableTransaction;
import com.assignment.analytics.ai.ActivityDigest.RuleFire;
import com.assignment.analytics.ai.ActivityDigest.TypeSummary;
import com.assignment.analytics.common.NotFoundException;
import com.assignment.analytics.domain.ActivityType;
import com.assignment.analytics.domain.CardActivity;
import com.assignment.analytics.domain.CryptoActivity;
import com.assignment.analytics.domain.Customer;
import com.assignment.analytics.domain.PaymentActivity;
import com.assignment.analytics.domain.RiskAssessment;
import com.assignment.analytics.domain.RiskRule;
import com.assignment.analytics.domain.Transaction;
import com.assignment.analytics.domain.TransactionStatus;
import com.assignment.analytics.repo.CardActivityRepository;
import com.assignment.analytics.repo.CryptoActivityRepository;
import com.assignment.analytics.repo.CustomerRepository;
import com.assignment.analytics.repo.PaymentActivityRepository;
import com.assignment.analytics.repo.RiskAssessmentRepository;
import com.assignment.analytics.repo.RiskRuleRepository;
import com.assignment.analytics.repo.TransactionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Compacts a customer's full activity into an {@link ActivityDigest}.
 * Pure data-shaping - no LLM involved. Keeping this deterministic and small
 * bounds prompt size and keeps every fact in the prompt traceable to the DB.
 */
@Component
public class ActivityDigestBuilder {

    private static final int MAX_NOTABLE = 18;
    private static final int MAX_NOTABLE_WITHOUT_RULES = 5;

    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final CardActivityRepository cardActivities;
    private final PaymentActivityRepository paymentActivities;
    private final CryptoActivityRepository cryptoActivities;
    private final RiskAssessmentRepository riskAssessments;
    private final RiskRuleRepository riskRules;

    public ActivityDigestBuilder(CustomerRepository customers, TransactionRepository transactions,
                                 CardActivityRepository cardActivities, PaymentActivityRepository paymentActivities,
                                 CryptoActivityRepository cryptoActivities, RiskAssessmentRepository riskAssessments,
                                 RiskRuleRepository riskRules) {
        this.customers = customers;
        this.transactions = transactions;
        this.cardActivities = cardActivities;
        this.paymentActivities = paymentActivities;
        this.cryptoActivities = cryptoActivities;
        this.riskAssessments = riskAssessments;
        this.riskRules = riskRules;
    }

    @Transactional(readOnly = true)
    public ActivityDigest build(UUID customerId) {
        Customer customer = customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
        List<Transaction> txs = transactions.findByCustomerIdOrderByCreatedAtDesc(customerId);
        List<UUID> txIds = txs.stream().map(Transaction::getTransactionId).toList();

        Map<UUID, CardActivity> cards = txIds.isEmpty() ? Map.of() : cardActivities.findByTransactionIdIn(txIds)
                .stream().collect(Collectors.toMap(CardActivity::getTransactionId, Function.identity()));
        Map<UUID, PaymentActivity> payments = txIds.isEmpty() ? Map.of() : paymentActivities.findByTransactionIdIn(txIds)
                .stream().collect(Collectors.toMap(PaymentActivity::getTransactionId, Function.identity()));
        Map<UUID, CryptoActivity> cryptos = txIds.isEmpty() ? Map.of() : cryptoActivities.findByTransactionIdIn(txIds)
                .stream().collect(Collectors.toMap(CryptoActivity::getTransactionId, Function.identity()));
        List<RiskAssessment> assessments = txIds.isEmpty() ? List.of() : riskAssessments.findByTransactionIdIn(txIds);
        Map<UUID, RiskRule> rulesById = riskRules.findAll().stream()
                .collect(Collectors.toMap(RiskRule::getRuleId, Function.identity()));

        List<TypeSummary> typeSummaries = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getActivityType, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(s -> s.type().name()))
                .toList();

        Map<UUID, List<RiskAssessment>> byRule = assessments.stream()
                .collect(Collectors.groupingBy(RiskAssessment::getRuleId));
        List<RuleFire> ruleFires = byRule.entrySet().stream()
                .map(e -> {
                    RiskRule rule = rulesById.get(e.getKey());
                    BigDecimal total = e.getValue().stream().map(RiskAssessment::getScoreContribution)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new RuleFire(
                            rule != null ? rule.getRuleName() : e.getKey().toString(),
                            rule != null ? rule.getAppliesTo() : "?",
                            rule != null ? rule.getThresholdLogic() : "",
                            rule != null ? rule.getWeight() : BigDecimal.ZERO,
                            e.getValue().size(), total);
                })
                .sorted(Comparator.comparing(RuleFire::totalContribution).reversed())
                .toList();
        BigDecimal totalRiskScore = assessments.stream().map(RiskAssessment::getScoreContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Long> corridors = txs.stream()
                .map(t -> payments.get(t.getTransactionId()))
                .filter(p -> p != null)
                .collect(Collectors.groupingBy(
                        p -> p.getPaymentMethod() + "->" + p.getReceiverBankCountry(),
                        TreeMap::new, Collectors.counting()));

        Map<String, Long> mccCounts = txs.stream()
                .map(t -> cards.get(t.getTransactionId()))
                .filter(c -> c != null)
                .collect(Collectors.groupingBy(CardActivity::getMccCode, TreeMap::new, Collectors.counting()));

        List<String> cryptoCounterparties = txs.stream()
                .map(t -> cryptos.get(t.getTransactionId()))
                .filter(c -> c != null)
                .map(c -> c.getExchangeName() != null
                        ? "exchange: " + c.getExchangeName()
                        : "wallet: " + c.getWalletAddressTo())
                .distinct()
                .sorted()
                .toList();

        Map<UUID, List<String>> firedNamesByTx = assessments.stream()
                .collect(Collectors.groupingBy(RiskAssessment::getTransactionId,
                        Collectors.mapping(a -> {
                            RiskRule rule = rulesById.get(a.getRuleId());
                            return rule != null ? rule.getRuleName() : a.getRuleId().toString();
                        }, Collectors.toList())));

        List<NotableTransaction> notable = selectNotable(txs, cards, payments, cryptos, firedNamesByTx);

        return new ActivityDigest(
                customer.getCustomerId(), customer.getFullName(), customer.getSegment(), customer.getCountry(),
                customer.getCreatedAt(),
                txs.isEmpty() ? null : txs.get(txs.size() - 1).getCreatedAt(),
                txs.isEmpty() ? null : txs.get(0).getCreatedAt(),
                txs.size(), typeSummaries, ruleFires, totalRiskScore,
                corridors, mccCounts, cryptoCounterparties, notable);
    }

    /**
     * Notable = every transaction that fired a rule (they carry the risk story)
     * plus the largest few unflagged ones for context, newest first, capped.
     */
    private List<NotableTransaction> selectNotable(List<Transaction> txs,
                                                   Map<UUID, CardActivity> cards,
                                                   Map<UUID, PaymentActivity> payments,
                                                   Map<UUID, CryptoActivity> cryptos,
                                                   Map<UUID, List<String>> firedNamesByTx) {
        List<Transaction> flagged = txs.stream()
                .filter(t -> firedNamesByTx.containsKey(t.getTransactionId()))
                .limit(MAX_NOTABLE)
                .toList();
        List<Transaction> context = txs.stream()
                .filter(t -> !firedNamesByTx.containsKey(t.getTransactionId()))
                .sorted(Comparator.comparing(Transaction::getAmount).reversed())
                .limit(Math.max(0, Math.min(MAX_NOTABLE_WITHOUT_RULES, MAX_NOTABLE - flagged.size())))
                .toList();
        List<Transaction> selected = new ArrayList<>(flagged);
        selected.addAll(context);
        selected.sort(Comparator.comparing(Transaction::getCreatedAt).reversed());
        return selected.stream()
                .map(t -> new NotableTransaction(t.getTransactionId(), t.getActivityType(), t.getCreatedAt(),
                        t.getAmount(), t.getCurrency(), t.getStatus().name(),
                        descriptor(t, cards.get(t.getTransactionId()), payments.get(t.getTransactionId()),
                                cryptos.get(t.getTransactionId())),
                        firedNamesByTx.getOrDefault(t.getTransactionId(), List.of())))
                .toList();
    }

    private String descriptor(Transaction t, CardActivity card, PaymentActivity payment, CryptoActivity crypto) {
        if (card != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(card.getMerchantName()).append(" (MCC ").append(card.getMccCode()).append(", ")
                    .append(card.isCardPresent() ? "card-present" : "card-not-present").append(")");
            if (card.getDeclineReason() != null) {
                sb.append(", declined: ").append(card.getDeclineReason());
            }
            return sb.toString();
        }
        if (payment != null) {
            return payment.getPaymentMethod() + " to " + payment.getReceiverBankCountry()
                    + " (" + payment.getSenderAccount() + " -> " + payment.getReceiverAccount() + ")";
        }
        if (crypto != null) {
            String via = crypto.getExchangeName() != null ? " via " + crypto.getExchangeName() : " (no exchange)";
            return crypto.getBlockchain() + " " + crypto.getWalletAddressFrom() + " -> "
                    + crypto.getWalletAddressTo() + via;
        }
        return t.getActivityType().name();
    }

    private TypeSummary summarize(ActivityType type, List<Transaction> txs) {
        Map<String, BigDecimal> totals = new TreeMap<>();
        long completed = 0;
        long pending = 0;
        long failed = 0;
        long reversed = 0;
        for (Transaction t : txs) {
            if (t.getStatus() == TransactionStatus.COMPLETED) {
                totals.merge(t.getCurrency(), t.getAmount(), BigDecimal::add);
            }
            switch (t.getStatus()) {
                case COMPLETED -> completed++;
                case PENDING -> pending++;
                case FAILED -> failed++;
                case REVERSED -> reversed++;
            }
        }
        return new TypeSummary(type, txs.size(), totals, completed, pending, failed, reversed);
    }
}
