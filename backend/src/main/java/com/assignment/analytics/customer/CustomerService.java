package com.assignment.analytics.customer;

import com.assignment.analytics.common.NotFoundException;
import com.assignment.analytics.customer.CustomerDtos.ActivityTypeSummary;
import com.assignment.analytics.customer.CustomerDtos.CardDetails;
import com.assignment.analytics.customer.CustomerDtos.CryptoDetails;
import com.assignment.analytics.customer.CustomerDtos.CustomerOverview;
import com.assignment.analytics.customer.CustomerDtos.CustomerSearchItem;
import com.assignment.analytics.customer.CustomerDtos.FiredRule;
import com.assignment.analytics.customer.CustomerDtos.FiredRuleSummary;
import com.assignment.analytics.customer.CustomerDtos.PaymentDetails;
import com.assignment.analytics.customer.CustomerDtos.TransactionItem;
import com.assignment.analytics.customer.CustomerDtos.TransactionsPage;
import com.assignment.analytics.domain.ActivityType;
import com.assignment.analytics.domain.Customer;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customers;
    private final TransactionRepository transactions;
    private final CardActivityRepository cardActivities;
    private final PaymentActivityRepository paymentActivities;
    private final CryptoActivityRepository cryptoActivities;
    private final RiskAssessmentRepository riskAssessments;
    private final RiskRuleRepository riskRules;

    public CustomerService(CustomerRepository customers, TransactionRepository transactions,
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

    public List<CustomerSearchItem> search(String query) {
        List<Customer> found = (query == null || query.isBlank())
                ? customers.findAll(Sort.by("fullName"))
                : customers.search(query.trim());
        return found.stream()
                .map(c -> new CustomerSearchItem(c.getCustomerId(), c.getFullName(), c.getSegment(),
                        c.getCountry(), transactions.findByCustomerIdOrderByCreatedAtDesc(c.getCustomerId()).size()))
                .toList();
    }

    public Customer getCustomer(UUID customerId) {
        return customers.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    public CustomerOverview overview(UUID customerId) {
        Customer customer = getCustomer(customerId);
        List<Transaction> txs = transactions.findByCustomerIdOrderByCreatedAtDesc(customerId);
        List<UUID> txIds = txs.stream().map(Transaction::getTransactionId).toList();
        List<RiskAssessment> assessments = txIds.isEmpty() ? List.of() : riskAssessments.findByTransactionIdIn(txIds);
        Map<UUID, RiskRule> rulesById = riskRules.findAll().stream()
                .collect(Collectors.toMap(RiskRule::getRuleId, Function.identity()));

        List<ActivityTypeSummary> activity = txs.stream()
                .collect(Collectors.groupingBy(Transaction::getActivityType, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> summarize(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(s -> s.type().name()))
                .toList();

        Map<UUID, List<RiskAssessment>> byRule = assessments.stream()
                .collect(Collectors.groupingBy(RiskAssessment::getRuleId));
        List<FiredRuleSummary> firedRules = byRule.entrySet().stream()
                .map(e -> {
                    RiskRule rule = rulesById.get(e.getKey());
                    BigDecimal total = e.getValue().stream()
                            .map(RiskAssessment::getScoreContribution)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return new FiredRuleSummary(
                            rule != null ? rule.getRuleName() : e.getKey().toString(),
                            rule != null ? rule.getAppliesTo() : "?",
                            e.getValue().size(), total);
                })
                .sorted(Comparator.comparing(FiredRuleSummary::totalContribution).reversed())
                .toList();

        BigDecimal riskScore = assessments.stream()
                .map(RiskAssessment::getScoreContribution)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        LocalDateTime first = txs.isEmpty() ? null : txs.get(txs.size() - 1).getCreatedAt();
        LocalDateTime last = txs.isEmpty() ? null : txs.get(0).getCreatedAt();

        return new CustomerOverview(customer.getCustomerId(), customer.getFullName(), customer.getEmail(),
                customer.getSegment(), customer.getCountry(), customer.getCreatedAt(), riskScore,
                txs.size(), first, last, activity, firedRules);
    }

    public TransactionsPage transactionsPage(UUID customerId, ActivityType type, int page, int size) {
        getCustomer(customerId);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Transaction> result = type == null
                ? transactions.findByCustomerId(customerId, pageable)
                : transactions.findByCustomerIdAndActivityType(customerId, type, pageable);
        List<TransactionItem> items = toItems(result.getContent());
        return new TransactionsPage(items, page, size, result.getTotalElements(), result.getTotalPages());
    }

    /** Assembles transaction DTOs with subtype details and fired rules using bulk lookups (no N+1). */
    public List<TransactionItem> toItems(List<Transaction> txs) {
        List<UUID> ids = txs.stream().map(Transaction::getTransactionId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, CardDetails> cards = cardActivities.findByTransactionIdIn(ids).stream()
                .collect(Collectors.toMap(c -> c.getTransactionId(),
                        c -> new CardDetails(c.getCardPan(), c.getCardType(), c.getMerchantName(), c.getMccCode(),
                                c.isCardPresent(), c.getAuthorizationCode(), c.getDeclineReason())));
        Map<UUID, PaymentDetails> payments = paymentActivities.findByTransactionIdIn(ids).stream()
                .collect(Collectors.toMap(p -> p.getTransactionId(),
                        p -> new PaymentDetails(p.getPaymentMethod(), p.getSenderAccount(), p.getReceiverAccount(),
                                p.getReceiverBankCountry())));
        Map<UUID, CryptoDetails> cryptos = cryptoActivities.findByTransactionIdIn(ids).stream()
                .collect(Collectors.toMap(c -> c.getTransactionId(),
                        c -> new CryptoDetails(c.getBlockchain(), c.getWalletAddressFrom(), c.getWalletAddressTo(),
                                c.getTxHash(), c.getExchangeName())));
        Map<UUID, RiskRule> rulesById = riskRules.findAll().stream()
                .collect(Collectors.toMap(RiskRule::getRuleId, Function.identity()));
        Map<UUID, List<FiredRule>> fired = riskAssessments.findByTransactionIdIn(ids).stream()
                .collect(Collectors.groupingBy(RiskAssessment::getTransactionId,
                        Collectors.mapping(a -> {
                            RiskRule rule = rulesById.get(a.getRuleId());
                            return new FiredRule(rule != null ? rule.getRuleName() : a.getRuleId().toString(),
                                    a.getScoreContribution());
                        }, Collectors.toList())));

        return txs.stream()
                .map(t -> new TransactionItem(t.getTransactionId(), t.getActivityType(), t.getAmount(),
                        t.getCurrency(), t.getStatus(), t.getCreatedAt(),
                        cards.get(t.getTransactionId()), payments.get(t.getTransactionId()),
                        cryptos.get(t.getTransactionId()),
                        fired.getOrDefault(t.getTransactionId(), List.of())))
                .toList();
    }

    private ActivityTypeSummary summarize(ActivityType type, List<Transaction> txs) {
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
        return new ActivityTypeSummary(type, txs.size(), totals, completed, pending, failed, reversed);
    }
}
