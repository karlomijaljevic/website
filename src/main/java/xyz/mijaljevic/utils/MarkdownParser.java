package xyz.mijaljevic.utils;

import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.AttributeProviderContext;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import xyz.mijaljevic.domain.dto.BlogMetadata;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Utility class for parsing Markdown files and rendering them to HTML.
 */
public final class MarkdownParser {
    /**
     * Custom attribute provider for the Markdown to HTML renderer. It adds
     * custom attributes to some HTML tags.
     */
    private static class BlogAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(
                final Node node,
                final String tagName,
                final Map<String, String> attributes
        ) {
            if (node instanceof Heading heading) {
                if (heading.getLevel() == 1) {
                    attributes.put("class", "page-title");
                }
            }
            if (node instanceof Link) {
                attributes.put("target", "_blank");
            }
        }
    }

    /**
     * Custom renderer for indented code blocks. This renderer ensures that
     * indented code blocks are wrapped in <pre> tags without any additional
     * <code> tags, preserving the original formatting.
     */
    private static class FencedCodeBlockNodeRenderer implements NodeRenderer {
        /**
         * Rendering context supplying attribute extension and the writer.
         */
        private final HtmlNodeRendererContext context;

        /**
         * Writer used to emit the rendered HTML.
         */
        private final HtmlWriter html;

        FencedCodeBlockNodeRenderer(
                @Nonnull final HtmlNodeRendererContext context
        ) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Nonnull
        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(FencedCodeBlock.class);
        }

        @Override
        public void render(final Node node) {
            FencedCodeBlock codeBlock = (FencedCodeBlock) node;
            html.line();
            html.tag(
                    "div",
                    context.extendAttributes(
                            codeBlock,
                            "div",
                            Map.of("class", "code-container")
                    )
            );
            html.tag(
                    "div",
                    context.extendAttributes(
                            codeBlock,
                            "div",
                            Map.of("class", "code-header")
                    )
            );
            html.tag(
                    "span",
                    context.extendAttributes(
                            codeBlock,
                            "span",
                            Map.of("class", "code-language")
                    )
            );
            if (codeBlock.getInfo() != null && !codeBlock.getInfo().isBlank()) {
                html.text(codeBlock.getInfo().trim());
            } else {
                html.text("Code");
            }
            html.tag("/span");
            html.tag(
                    "button",
                    context.extendAttributes(
                            codeBlock,
                            "button",
                            Map.of(
                                    "class", "copy-button",
                                    "onclick", "copyCode(this)"
                            )
                    )
            );
            html.text("Copy");
            html.tag("/button");
            html.tag("/div");
            html.tag("pre");
            html.text(codeBlock.getLiteral());
            html.tag("/pre");
            html.tag("/div");
            html.line();
        }
    }

    private MarkdownParser() {
        // NOTE: Utility class, not meant to be instantiated.
    }

    /**
     * Markdown parser instance
     */
    public static final Parser MD_PARSER = Parser.builder().build();

    /**
     * Markdown to HTML renderer instance
     *
     * <p>
     * The {@link SuppressWarnings} <code>unused</code> is for the
     * {@link AttributeProviderContext} context utilized in the lambda.
     * </p>
     */
    @SuppressWarnings("unused")
    public static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder()
            .attributeProviderFactory( context -> new BlogAttributeProvider())
            .nodeRendererFactory(FencedCodeBlockNodeRenderer::new)
            .build();

    /**
     * The fence delimiting the front-matter metadata block, both opening and
     * closing.
     */
    private static final String FRONT_MATTER_FENCE = "---";

    /**
     * Formatter for the {@code Date} and {@code Updated} metadata values, e.g.
     * {@code 27-Jan-2026}. Matches the site's display date pattern.
     */
    private static final DateTimeFormatter METADATA_DATE_PATTERN =
            DateTimeFormatter.ofPattern("d-MMM-uuuu", Locale.ENGLISH);

    /**
     * Renders a Markdown file to HTML. The optional front-matter metadata block
     * is stripped before rendering so it never leaks into the rendered body.
     *
     * @param file A Markdown file to render.
     * @return A {@link String} containing the HTML representation of the
     * provided Markdown file or null in case of a failure.
     * @throws NullPointerException if {@code file} is null.
     */
    @Nullable
    public static String renderMarkdownToHtml(final File file) {
        Objects.requireNonNull(file, "file must not be null");

        final String content = readFileContent(file);

        if (content == null) {
            return null;
        }

        try {
            final Node document = MD_PARSER.parse(stripFrontMatter(content));

            return MD_RENDERER.render(document);
        } catch (RuntimeException e) {
            Log.warnf(e, "Failed to render markdown file to HTML: %s", file);
            return null;
        }
    }

    /**
     * Parses the front-matter metadata block at the top of a Markdown file.
     *
     * @param file A Markdown file to read the metadata from.
     * @return The parsed {@link BlogMetadata}, or {@link BlogMetadata#EMPTY}
     * when the file cannot be read or carries no metadata block.
     * @throws NullPointerException if {@code file} is null.
     */
    @Nonnull
    public static BlogMetadata parseMetadata(final File file) {
        Objects.requireNonNull(file, "file must not be null");

        final String content = readFileContent(file);

        if (content == null) {
            return BlogMetadata.EMPTY;
        }

        return parseFrontMatter(content);
    }

    /**
     * Extracts the title from a Markdown file. The title is assumed to be the
     * first heading in the file. The front-matter metadata block, if present,
     * is stripped first so it is never mistaken for the heading. Used as a
     * fallback when the {@code Title} metadata tag is absent.
     *
     * @param file A Markdown file to extract the title from.
     * @return A {@link String} containing the title or "Untitled" in case of
     * failure or if no title is found.
     * @throws NullPointerException if {@code file} is null.
     */
    @Nonnull
    public static String getTitleFromFile(final File file) {
        Objects.requireNonNull(file, "file must not be null");

        final String content = readFileContent(file);

        if (content == null) {
            return "Untitled";
        }

        final Node node = MD_PARSER.parse(stripFrontMatter(content));
        final Node first = node.getFirstChild();

        if (first == null) {
            return "Untitled";
        }

        final String title = MD_RENDERER.render(first)
                .replaceAll("<[^>]+>", "")
                .trim();

        return title.isEmpty() ? "Untitled" : title;
    }

    /**
     * Reads the full textual content of a Markdown file.
     *
     * @param file A Markdown file to read.
     * @return The file content, or null in case of a read failure.
     */
    @Nullable
    private static String readFileContent(final File file) {
        try {
            return Files.readString(file.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Log.warnf(e, "Failed to read markdown file: %s", file);
            return null;
        }
    }

    /**
     * Returns the Markdown body with the leading front-matter metadata block
     * removed. When no well-formed block is present the content is returned
     * unchanged.
     *
     * @param content The full file content.
     * @return The Markdown body without the metadata block.
     */
    @Nonnull
    private static String stripFrontMatter(final String content) {
        final String[] lines = content.split("\n", -1);
        final int close = locateClosingFence(lines);

        if (close < 0) {
            return content;
        }

        return String.join("\n", List.of(lines).subList(close + 1, lines.length));
    }

    /**
     * Parses the front-matter metadata block into a {@link BlogMetadata}.
     *
     * @param content The full file content.
     * @return The parsed metadata, or {@link BlogMetadata#EMPTY} when no
     * well-formed block is present.
     */
    @Nonnull
    private static BlogMetadata parseFrontMatter(final String content) {
        final String[] lines = content.split("\n", -1);
        final int close = locateClosingFence(lines);

        if (close < 0) {
            return BlogMetadata.EMPTY;
        }

        String title = null;
        String author = null;
        LocalDate date = null;
        LocalDate updated = null;
        List<String> tags = List.of();

        for (int i = 1; i < close; i++) {
            final String line = lines[i];
            final int colon = line.indexOf(':');

            if (colon < 0) {
                continue;
            }

            final String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            final String value = line.substring(colon + 1).trim();

            if (value.isEmpty()) {
                continue;
            }

            switch (key) {
                case "title" -> title = value;
                case "author" -> author = value;
                case "date" -> date = parseMetadataDate(value);
                case "updated" -> updated = parseMetadataDate(value);
                case "tags" -> tags = parseTags(value);
                default -> { /* Unknown key, ignored. */ }
            }
        }

        return new BlogMetadata(title, author, date, updated, tags);
    }

    /**
     * Locates the closing fence of a front-matter block. A block is recognized
     * only when the first non-blank line is the fence and a later matching
     * fence closes it.
     *
     * @param lines The file content split into lines.
     * @return The index of the closing fence line, or -1 when no well-formed
     * block is present.
     */
    private static int locateClosingFence(final String[] lines) {
        int open = 0;

        while (open < lines.length && lines[open].trim().isEmpty()) {
            open++;
        }

        if (open >= lines.length || !lines[open].trim().equals(FRONT_MATTER_FENCE)) {
            return -1;
        }

        for (int i = open + 1; i < lines.length; i++) {
            if (lines[i].trim().equals(FRONT_MATTER_FENCE)) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Parses a {@code Date}/{@code Updated} metadata value such as
     * {@code 27-Jan-2026}, falling back to the ISO {@code uuuu-MM-dd} form.
     *
     * @param value The raw date value.
     * @return The parsed {@link LocalDate}, or null when it cannot be parsed.
     */
    @Nullable
    private static LocalDate parseMetadataDate(final String value) {
        try {
            return LocalDate.parse(value, METADATA_DATE_PATTERN);
        } catch (DateTimeParseException primaryException) {
            try {
                return LocalDate.parse(value);
            } catch (DateTimeParseException isoException) {
                Log.warnf("Unparseable date metadata value: '%s'", value);
                return null;
            }
        }
    }

    /**
     * Parses a comma-separated {@code Tags} metadata value into a list of
     * trimmed, non-blank tags.
     *
     * @param value The raw tags value.
     * @return The parsed tags, never null.
     */
    @Nonnull
    private static List<String> parseTags(final String value) {
        final List<String> tags = new ArrayList<>();

        for (final String tag : value.split(",")) {
            final String trimmed = tag.trim();

            if (!trimmed.isEmpty()) {
                tags.add(trimmed);
            }
        }

        return tags;
    }
}
