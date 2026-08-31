package com.assignment.analytics.repo;

import com.assignment.analytics.domain.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OperatorRepository extends JpaRepository<Operator, UUID> {

    Optional<Operator> findByUsername(String username);
}
