package com.assignment.analytics.rag;

import com.assignment.analytics.domain.PolicyChunk;
import com.assignment.analytics.domain.PolicyDocument;
import com.assignment.analytics.repo.PolicyChunkRepository;
import com.assignment.analytics.repo.PolicyDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Loads the unstructured policy corpus (classpath:policies/*.md) into the
 * database at startup, chunked by markdown section, then (re)builds the BM25
 * index. Loading is idempotent: unchanged files (by content hash) are skipped.
 * The markdown files stay the single source of truth for policy content.
 */
@Component
public class PolicyCorpusLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicyCorpusLoader.class);

    private final PolicyDocumentRepository documentRepository;
    private final PolicyChunkRepository chunkRepository;
    private final PolicyRetriever retriever;

    public PolicyCorpusLoader(PolicyDocumentRepository documentRepository, PolicyChunkRepository chunkRepository,
                              PolicyRetriever retriever) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.retriever = retriever;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver()
                .getResources("classpath:policies/*.md");
        for (Resource resource : resources) {
            loadDocument(resource);
        }
        retriever.rebuild();
        log.info("Policy corpus ready: {} documents, {} chunks indexed",
                resources.length, retriever.corpusSize());
    }

    private void loadDocument(Resource resource) throws IOException {
        String sourceFile = resource.getFilename();
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        String sha256 = sha256(content);

        PolicyDocument existing = documentRepository.findBySourceFile(sourceFile).orElse(null);
        if (existing != null && existing.getContentSha256().equals(sha256)) {
            return;
        }
        UUID documentId = stableId("doc:" + sourceFile);
        if (existing != null) {
            chunkRepository.deleteByDocumentId(existing.getDocumentId());
            documentRepository.delete(existing);
            documentRepository.flush();
        }

        List<Section> sections = splitSections(content);
        String title = sections.isEmpty() || sections.get(0).documentTitle == null
                ? sourceFile
                : sections.get(0).documentTitle;
        documentRepository.save(new PolicyDocument(documentId, title, sourceFile, sha256, LocalDateTime.now()));
        int index = 0;
        for (Section section : sections) {
            if (section.body.isBlank()) {
                continue;
            }
            chunkRepository.save(new PolicyChunk(
                    stableId("chunk:" + sourceFile + "#" + index), documentId, index,
                    section.sectionTitle, section.body.strip()));
            index++;
        }
        log.info("Loaded policy document '{}' ({} chunks)", title, index);
    }

    record Section(String documentTitle, String sectionTitle, String body) {
    }

    /**
     * Splits a markdown document into one chunk per "## " section. The H1 line
     * provides the document title; any preamble before the first section
     * becomes its own chunk.
     */
    static List<Section> splitSections(String markdown) {
        List<Section> sections = new ArrayList<>();
        String documentTitle = null;
        String currentSection = null;
        StringBuilder body = new StringBuilder();
        for (String line : markdown.split("\n", -1)) {
            if (line.startsWith("# ") && documentTitle == null) {
                documentTitle = line.substring(2).strip();
            } else if (line.startsWith("## ")) {
                sections.add(new Section(documentTitle, currentSection, body.toString()));
                currentSection = line.substring(3).strip();
                body = new StringBuilder();
            } else {
                body.append(line).append('\n');
            }
        }
        sections.add(new Section(documentTitle, currentSection, body.toString()));
        return sections;
    }

    private static UUID stableId(String key) {
        return UUID.nameUUIDFromBytes(key.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
