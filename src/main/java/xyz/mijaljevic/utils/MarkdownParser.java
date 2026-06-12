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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
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
     * Renders a Markdown file to HTML.
     *
     * @param file A Markdown file to render.
     * @return A {@link String} containing the HTML representation of the
     * provided Markdown file or null in case of a failure.
     * @throws NullPointerException if {@code file} is null.
     */
    @Nullable
    public static String renderMarkdownToHtml(final File file) {
        Objects.requireNonNull(file, "file must not be null");

        Node document = parseMarkdownFile(file);

        if (document == null) {
            return null;
        }

        try {
            return MD_RENDERER.render(document);
        } catch (RuntimeException e) {
            Log.warnf(e, "Failed to render markdown file to HTML: %s", file);
            return null;
        }
    }

    /**
     * Extracts the title from a Markdown file. The title is assumed to be the
     * first heading in the file.
     *
     * @param file A Markdown file to extract the title from.
     * @return A {@link String} containing the title or "Untitled" in case of
     * failure or if no title is found.
     * @throws NullPointerException if {@code file} is null.
     */
    @Nonnull
    public static String getTitleFromFile(final File file) {
        Objects.requireNonNull(file, "file must not be null");

        Node node = parseMarkdownFile(file);

        if (node == null) return "Untitled";

        return MD_RENDERER.render(node.getFirstChild())
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    /**
     * Parses a Markdown file into an {@link Node} instance.
     *
     * @param file A Markdown file to parse.
     * @return A {@link Node} instance representing the Markdown file or null
     * in case of a failure.
     */
    @Nullable
    private static Node parseMarkdownFile(final File file) {
        try (BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return MD_PARSER.parseReader(reader);
        } catch (IOException e) {
            Log.warnf(e, "Failed to read markdown file: %s", file);
            return null;
        }
    }
}
