package com.assignment.analytics.rag;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class Bm25IndexTest {

    private final UUID crypto = UUID.randomUUID();
    private final UUID cards = UUID.randomUUID();
    private final UUID jurisdictions = UUID.randomUUID();

    private Bm25Index index() {
        return new Bm25Index(List.of(
                new Bm25Index.Doc(crypto,
                        "Transfers to known mixing or tumbling services obscure the source of funds. "
                                + "Mixer addresses require same-day escalation."),
                new Bm25Index.Doc(cards,
                        "Three or more declined card authorizations within 60 minutes indicate card testing."),
                new Bm25Index.Doc(jurisdictions,
                        "Payments to greylist jurisdictions require enhanced due diligence and source of funds checks.")));
    }

    @Test
    void ranksTheTopicallyMatchingChunkFirst() {
        List<Bm25Index.Scored> result = index().search("mixing tumbling service escalation", 3);
        assertThat(result).isNotEmpty();
        assertThat(result.get(0).chunkId()).isEqualTo(crypto);
    }

    @Test
    void matchesPluralAndSingularForms() {
        // "jurisdiction" (singular) should still hit the chunk that says "jurisdictions".
        List<Bm25Index.Scored> result = index().search("greylist jurisdiction", 3);
        assertThat(result.get(0).chunkId()).isEqualTo(jurisdictions);
    }

    @Test
    void ignoresStopwordOnlyQueries() {
        assertThat(index().search("the of and to", 3)).isEmpty();
    }

    @Test
    void emptyIndexReturnsNoResults() {
        assertThat(new Bm25Index(List.of()).search("anything", 5)).isEmpty();
    }

    @Test
    void limitsResultCount() {
        assertThat(index().search("funds source", 1)).hasSize(1);
    }
}
