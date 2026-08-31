package com.assignment.analytics.rag;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyCorpusLoaderTest {

    @Test
    void splitsMarkdownIntoTitledSections() {
        String markdown = """
                # My Policy

                Preamble text.

                ## First Section

                Body one.

                ## Second Section

                Body two.
                """;
        var sections = PolicyCorpusLoader.splitSections(markdown);

        assertThat(sections).hasSize(3);
        assertThat(sections.get(0).documentTitle()).isEqualTo("My Policy");
        assertThat(sections.get(0).sectionTitle()).isNull();
        assertThat(sections.get(0).body()).contains("Preamble text.");
        assertThat(sections.get(1).sectionTitle()).isEqualTo("First Section");
        assertThat(sections.get(1).body()).contains("Body one.");
        assertThat(sections.get(2).sectionTitle()).isEqualTo("Second Section");
    }

    @Test
    void handlesDocumentWithoutSections() {
        var sections = PolicyCorpusLoader.splitSections("# Title\nJust text\n");
        assertThat(sections).hasSize(1);
        assertThat(sections.get(0).documentTitle()).isEqualTo("Title");
        assertThat(sections.get(0).body()).contains("Just text");
    }

    @Test
    void realCorpusChunksAreEmittedInOrder(){
        var sections = PolicyCorpusLoader.splitSections("""
                # T
                ## A
                a
                ## B
                b
                """);
        assertThat(sections).extracting(s -> s.sectionTitle()).containsExactly(null, "A", "B");
    }
}
