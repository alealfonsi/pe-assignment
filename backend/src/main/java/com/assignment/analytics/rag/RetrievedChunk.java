package com.assignment.analytics.rag;

import java.util.UUID;

/** One policy excerpt returned by retrieval, ready for prompt injection and UI display. */
public record RetrievedChunk(UUID chunkId, String documentTitle, String sectionTitle, String content,
                             double score) {
}
