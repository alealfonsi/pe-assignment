package com.assignment.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "policy_chunks")
public class PolicyChunk {

    @Id
    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(name = "section_title")
    private String sectionTitle;

    @Column(nullable = false)
    private String content;

    protected PolicyChunk() {
    }

    public PolicyChunk(UUID chunkId, UUID documentId, int chunkIndex, String sectionTitle, String content) {
        this.chunkId = chunkId;
        this.documentId = documentId;
        this.chunkIndex = chunkIndex;
        this.sectionTitle = sectionTitle;
        this.content = content;
    }

    public UUID getChunkId() {
        return chunkId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public int getChunkIndex() {
        return chunkIndex;
    }

    public String getSectionTitle() {
        return sectionTitle;
    }

    public String getContent() {
        return content;
    }
}
