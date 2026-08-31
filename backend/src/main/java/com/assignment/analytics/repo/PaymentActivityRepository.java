package com.assignment.analytics.repo;

import com.assignment.analytics.domain.PaymentActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentActivityRepository extends JpaRepository<PaymentActivity, UUID> {

    List<PaymentActivity> findByTransactionIdIn(Collection<UUID> transactionIds);
}
