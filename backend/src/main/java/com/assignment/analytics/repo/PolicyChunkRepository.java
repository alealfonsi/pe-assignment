package com.assignment.analytics.repo;

import com.assignment.analytics.domain.PolicyChunk;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyChunkRepository extends JpaRepository<PolicyChunk, UUID> {

    List<PolicyChunk> findByDocumentId(UUID documentId);

    void deleteByDocumentId(UUID documentId);
}
