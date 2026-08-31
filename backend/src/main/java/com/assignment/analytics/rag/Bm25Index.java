package com.assignment.analytics.rag;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Small self-contained BM25 (Okapi) index over policy chunks.
 *
 * Lexical retrieval was chosen deliberately over embeddings: the corpus is a
 * few dozen chunks of terminology-dense policy text, the retrieval queries are
 * formulated by the LLM itself (so they use the same vocabulary), and it keeps
 * the application runnable with zero external services. The retrieval
 * interface is the seam where a vector store could be swapped in.
 */
public class Bm25Index {

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");
    private static final Set<String> STOPWORDS = Set.of(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "have", "in",
            "is", "it", "its", "of", "on", "or", "that", "the", "this", "to", "was", "were",
            "will", "with", "not", "must", "any", "all", "when", "which", "their", "they");

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    public record Doc(UUID chunkId, String text) {
    }

    public record Scored(UUID chunkId, double score) {
    }

    private final Map<String, Map<UUID, Integer>> postings = new HashMap<>();
    private final Map<UUID, Integer> docLengths = new HashMap<>();
    private double avgDocLength;

    public Bm25Index(List<Doc> docs) {
        for (Doc doc : docs) {
            List<String> terms = tokenize(doc.text());
            docLengths.put(doc.chunkId(), terms.size());
            for (String term : terms) {
                postings.computeIfAbsent(term, t -> new HashMap<>())
                        .merge(doc.chunkId(), 1, Integer::sum);
            }
        }
        avgDocLength = docLengths.values().stream().mapToInt(Integer::intValue).average().orElse(0);
    }

    public int size() {
        return docLengths.size();
    }

    public List<Scored> search(String query, int limit) {
        Map<UUID, Double> scores = new HashMap<>();
        int n = docLengths.size();
        if (n == 0) {
            return List.of();
        }
        for (String term : tokenize(query)) {
            Map<UUID, Integer> termPostings = postings.get(term);
            if (termPostings == null) {
                continue;
            }
            int df = termPostings.size();
            double idf = Math.log(1.0 + (n - df + 0.5) / (df + 0.5));
            for (Map.Entry<UUID, Integer> e : termPostings.entrySet()) {
                int tf = e.getValue();
                double docLen = docLengths.get(e.getKey());
                double norm = (tf * (K1 + 1)) / (tf + K1 * (1 - B + B * docLen / avgDocLength));
                scores.merge(e.getKey(), idf * norm, Double::sum);
            }
        }
        List<Scored> result = new ArrayList<>();
        scores.forEach((id, score) -> result.add(new Scored(id, score)));
        result.sort(Comparator.comparingDouble(Scored::score).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    static List<String> tokenize(String text) {
        List<String> terms = new ArrayList<>();
        for (String raw : NON_ALNUM.split(text.toLowerCase(Locale.ROOT))) {
            if (raw.isEmpty() || STOPWORDS.contains(raw)) {
                continue;
            }
            // Minimal plural folding so "transfers" matches "transfer".
            String term = raw.length() > 3 && raw.endsWith("s") && !raw.endsWith("ss")
                    ? raw.substring(0, raw.length() - 1)
                    : raw;
            terms.add(term);
        }
        return terms;
    }
}
