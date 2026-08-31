package com.assignment.analytics.repo;

import com.assignment.analytics.domain.CryptoActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CryptoActivityRepository extends JpaRepository<CryptoActivity, UUID> {

    List<CryptoActivity> findByTransactionIdIn(Collection<UUID> transactionIds);
}
