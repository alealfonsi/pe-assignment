package com.assignment.analytics.repo;

import com.assignment.analytics.domain.CardActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CardActivityRepository extends JpaRepository<CardActivity, UUID> {

    List<CardActivity> findByTransactionIdIn(Collection<UUID> transactionIds);
}
