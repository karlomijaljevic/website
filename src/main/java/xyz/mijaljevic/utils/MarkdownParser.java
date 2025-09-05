package xyz.mijaljevic.utils;

import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.Heading;
import org.commonmark.node.Link;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.AttributeProvider;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Utility class for parsing markdown files and rendering them to HTML.
 */
public final class MarkdownParser {
    /**
     * Custom attribute provider for the markdown to HTML renderer. It adds
     * custom attributes to some HTML tags.
     */
    private static class BlogAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(
                Node node,
                String tagName,
                Map<String, String> attributes
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
        private final HtmlNodeRendererContext context;
        private final HtmlWriter html;

        FencedCodeBlockNodeRenderer(HtmlNodeRendererContext context) {
            this.context = context;
            this.html = context.getWriter();
        }

        @Override
        public Set<Class<? extends Node>> getNodeTypes() {
            return Set.of(FencedCodeBlock.class);
        }

        @Override
        public void render(Node node) {
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
        // Utility class
    }

    /**
     * Markdown parser instance
     */
    public static final Parser MD_PARSER = Parser.builder().build();

    /**
     * Markdown to HTML renderer instance
     */
    public static final HtmlRenderer MD_RENDERER = HtmlRenderer.builder()
            .attributeProviderFactory(context -> new BlogAttributeProvider())
            .nodeRendererFactory(FencedCodeBlockNodeRenderer::new)
            .build();

    /**
     * Renders a markdown file to HTML.
     *
     * @param file A markdown file to render.
     * @return A {@link String} containing the HTML representation of the
     * provided markdown file or null in case of a failure.
     */
    public static String renderMarkdownToHtml(File file) {
        try {
            Node document = parseMarkdownFile(file);

            return MD_RENDERER.render(document);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extracts the title from a markdown file. The title is assumed to be the
     * first heading in the file.
     *
     * @param file A markdown file to extract the title from.
     * @return A {@link String} containing the title or "Untitled" in case of
     * failure or if no title is found.
     */
    public static String getTitleFromFile(File file) {
        Node node = parseMarkdownFile(file);

        if (node == null) return "Untitled";

        return MD_RENDERER.render(node.getFirstChild())
                .replaceAll("<[^>]+>", "")
                .trim();
    }

    /**
     * Parses a markdown file into an {@link Node} instance.
     *
     * @param file A markdown file to parse.
     * @return A {@link Node} instance representing the markdown file or null
     * in case of a failure.
     */
    private static Node parseMarkdownFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));

            Node document = MD_PARSER.parseReader(reader);

            reader.close();

            return document;
        } catch (IOException e) {
            return null;
        }
    }
}
