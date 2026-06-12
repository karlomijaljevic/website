package xyz.mijaljevic.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

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
