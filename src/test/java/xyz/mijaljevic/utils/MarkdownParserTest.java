package xyz.mijaljevic.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import xyz.mijaljevic.domain.dto.BlogMetadata;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class MarkdownParserTest {
    @TempDir
    Path tempDir;

    private File writeMarkdown(String content) throws Exception {
        Path path = tempDir.resolve("post.md");
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
        return path.toFile();
    }

    @Test
    @DisplayName("renderMarkdownToHtml applies the custom heading, link and code-block attributes")
    void renderMarkdownToHtml_appliesCustomAttributes() throws Exception {
        File file = writeMarkdown("""
                # Hello World

                Some text with a [link](https://example.com).

                ```java
                System.out.println("hi");
                ```
                """);

        String html = MarkdownParser.renderMarkdownToHtml(file);

        assertThat(html).isNotNull();
        // BlogAttributeProvider tags the H1 and opens links in a new tab.
        assertThat(html).contains("class=\"page-title\"");
        assertThat(html).contains("target=\"_blank\"");
        // FencedCodeBlockNodeRenderer wraps fenced code in the custom container.
        assertThat(html).contains("code-container");
        assertThat(html).contains("copy-button");
        // The code language label is taken from the fence info string.
        assertThat(html).contains("code-language\">java");
        // The fenced code literal is HTML-escaped inside the <pre> block.
        assertThat(html).contains("System.out.println(&quot;hi&quot;);");
    }

    @Test
    @DisplayName("getTitleFromFile returns the first heading with markup stripped")
    void getTitleFromFile_returnsFirstHeadingText() throws Exception {
        File file = writeMarkdown("""
                # Hello World

                Body paragraph.
                """);

        assertThat(MarkdownParser.getTitleFromFile(file)).isEqualTo("Hello World");
    }

    @Test
    @DisplayName("parseMetadata reads every front-matter field")
    void parseMetadata_readsAllFields() throws Exception {
        File file = writeMarkdown("""
                ---
                Title:    Best blog ever
                Author:   Donald Trump
                Date:     27-Jan-2026
                Updated:  29-Jan-2026
                Tags:     donald, metadata, tags, markdown
                ---
                # Best blog ever

                Body paragraph.
                """);

        BlogMetadata metadata = MarkdownParser.parseMetadata(file);

        assertThat(metadata.title()).isEqualTo("Best blog ever");
        assertThat(metadata.author()).isEqualTo("Donald Trump");
        assertThat(metadata.date()).isEqualTo(LocalDate.of(2026, 1, 27));
        assertThat(metadata.updated()).isEqualTo(LocalDate.of(2026, 1, 29));
        assertThat(metadata.tags())
                .containsExactly("donald", "metadata", "tags", "markdown");
    }

    @Test
    @DisplayName("parseMetadata leaves updated null when the Updated tag is absent")
    void parseMetadata_noUpdated_isNull() throws Exception {
        File file = writeMarkdown("""
                ---
                Title: No update
                Date:  27-Jan-2026
                ---
                # No update
                """);

        BlogMetadata metadata = MarkdownParser.parseMetadata(file);

        assertThat(metadata.date()).isEqualTo(LocalDate.of(2026, 1, 27));
        assertThat(metadata.updated()).isNull();
    }

    @Test
    @DisplayName("parseMetadata returns EMPTY when no front-matter block is present")
    void parseMetadata_noFrontMatter_returnsEmpty() throws Exception {
        File file = writeMarkdown("""
                # Hello World

                Body paragraph.
                """);

        BlogMetadata metadata = MarkdownParser.parseMetadata(file);

        assertThat(metadata.title()).isNull();
        assertThat(metadata.author()).isNull();
        assertThat(metadata.date()).isNull();
        assertThat(metadata.tags()).isEmpty();
    }

    @Test
    @DisplayName("parseMetadata leaves an unparseable Date null but keeps other fields")
    void parseMetadata_unparseableDate_isNull() throws Exception {
        File file = writeMarkdown("""
                ---
                Title: Dateless
                Date:  not a date
                ---
                # Dateless
                """);

        BlogMetadata metadata = MarkdownParser.parseMetadata(file);

        assertThat(metadata.title()).isEqualTo("Dateless");
        assertThat(metadata.date()).isNull();
    }

    @Test
    @DisplayName("renderMarkdownToHtml strips the front-matter block from the rendered body")
    void renderMarkdownToHtml_stripsFrontMatter() throws Exception {
        File file = writeMarkdown("""
                ---
                Title: Best blog ever
                Author: Donald Trump
                ---
                # Best blog ever

                Body paragraph.
                """);

        String html = MarkdownParser.renderMarkdownToHtml(file);

        assertThat(html).isNotNull();
        // The metadata never leaks into the rendered HTML.
        assertThat(html).doesNotContain("Donald Trump");
        assertThat(html).doesNotContain("Title:");
        // The body heading still renders as the page title.
        assertThat(html).contains("class=\"page-title\"");
        assertThat(html).contains("Best blog ever");
    }

    @Test
    @DisplayName("getTitleFromFile ignores the front-matter block and returns the heading")
    void getTitleFromFile_ignoresFrontMatter() throws Exception {
        File file = writeMarkdown("""
                ---
                Author: Donald Trump
                ---
                # Heading Title

                Body paragraph.
                """);

        assertThat(MarkdownParser.getTitleFromFile(file)).isEqualTo("Heading Title");
    }

    @Test
    @DisplayName("parseMetadata rejects a null file")
    void parseMetadata_null_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> MarkdownParser.parseMetadata(null));
    }

    @Test
    @DisplayName("renderMarkdownToHtml rejects a null file")
    void renderMarkdownToHtml_null_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> MarkdownParser.renderMarkdownToHtml(null));
    }

    @Test
    @DisplayName("getTitleFromFile rejects a null file")
    void getTitleFromFile_null_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> MarkdownParser.getTitleFromFile(null));
    }
}
