package com.assignment.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "policy_documents")
public class PolicyDocument {

    @Id
    @Column(name = "document_id")
    private UUID documentId;

    @Column(nullable = false)
    private String title;

    @Column(name = "source_file", nullable = false, unique = true)
    private String sourceFile;

    @Column(name = "content_sha256", nullable = false)
    private String contentSha256;

    @Column(name = "loaded_at", nullable = false)
    private LocalDateTime loadedAt;

    protected PolicyDocument() {
    }

    public PolicyDocument(UUID documentId, String title, String sourceFile, String contentSha256, LocalDateTime loadedAt) {
        this.documentId = documentId;
        this.title = title;
        this.sourceFile = sourceFile;
        this.contentSha256 = contentSha256;
        this.loadedAt = loadedAt;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getContentSha256() {
        return contentSha256;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }
}
