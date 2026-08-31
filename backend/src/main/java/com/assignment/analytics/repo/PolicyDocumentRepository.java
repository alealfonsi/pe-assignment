package com.assignment.analytics.repo;

import com.assignment.analytics.domain.PolicyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PolicyDocumentRepository extends JpaRepository<PolicyDocument, UUID> {

    Optional<PolicyDocument> findBySourceFile(String sourceFile);
}
