package com.assignment.analytics.rag;

import com.assignment.analytics.domain.PolicyChunk;
import com.assignment.analytics.domain.PolicyDocument;
import com.assignment.analytics.repo.PolicyChunkRepository;
import com.assignment.analytics.repo.PolicyDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Retrieval half of the RAG pipeline: BM25 search over the policy-chunk corpus.
 * The index is rebuilt from the database by {@link PolicyCorpusLoader} at
 * startup and kept in memory (the corpus is small and read-only at runtime).
 */
@Service
public class PolicyRetriever {

    private final PolicyChunkRepository chunkRepository;
    private final PolicyDocumentRepository documentRepository;

    private volatile Bm25Index index = new Bm25Index(List.of());
    private volatile Map<UUID, PolicyChunk> chunksById = Map.of();
    private volatile Map<UUID, String> documentTitles = Map.of();

    public PolicyRetriever(PolicyChunkRepository chunkRepository, PolicyDocumentRepository documentRepository) {
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
    }

    /** Rebuilds the in-memory index from the database. Called after corpus loading. */
    public synchronized void rebuild() {
        List<PolicyChunk> chunks = chunkRepository.findAll();
        chunksById = chunks.stream()
                .collect(Collectors.toUnmodifiableMap(PolicyChunk::getChunkId, Function.identity()));
        documentTitles = documentRepository.findAll().stream()
                .collect(Collectors.toUnmodifiableMap(PolicyDocument::getDocumentId, PolicyDocument::getTitle));
        index = new Bm25Index(chunks.stream()
                .map(c -> new Bm25Index.Doc(c.getChunkId(),
                        (c.getSectionTitle() == null ? "" : c.getSectionTitle() + "\n") + c.getContent()))
                .toList());
    }

    public int corpusSize() {
        return index.size();
    }

    /**
     * Runs each query against the index and merges results, deduplicating by
     * chunk and keeping each chunk's best score, then returns the global top-k.
     */
    public List<RetrievedChunk> retrieve(List<String> queries, int topK) {
        Map<UUID, Double> best = new HashMap<>();
        for (String query : queries) {
            for (Bm25Index.Scored scored : index.search(query, topK)) {
                best.merge(scored.chunkId(), scored.score(), Math::max);
            }
        }
        return best.entrySet().stream()
                .sorted(Map.Entry.<UUID, Double>comparingByValue(Comparator.reverseOrder()))
                .limit(topK)
                .map(e -> toRetrieved(e.getKey(), e.getValue()))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private RetrievedChunk toRetrieved(UUID chunkId, double score) {
        PolicyChunk chunk = chunksById.get(chunkId);
        String docTitle = documentTitles.getOrDefault(chunk.getDocumentId(), "Unknown document");
        return new RetrievedChunk(chunkId, docTitle, chunk.getSectionTitle(), chunk.getContent(), score);
    }
}
