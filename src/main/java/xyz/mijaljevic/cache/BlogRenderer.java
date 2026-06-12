package xyz.mijaljevic.cache;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.utils.MarkdownParser;

import java.io.File;

/**
 * Renders a blog Markdown file to HTML, caching the result so each file is
 * parsed at most once until it changes. Backed by Quarkus Cache (Caffeine),
 * keyed by the blog file name.
 */
@ApplicationScoped
public class BlogRenderer {
    /**
     * The path to the blogs' directory.
     */
    private final String blogsDirectoryPath;

    /**
     * Creates the renderer with the configured blogs directory path.
     *
     * @param blogsDirectoryPath The path to the blogs' directory.
     */
    @Inject
    public BlogRenderer(
            @ConfigProperty(
                    name = "application.blogs-directory",
                    defaultValue = "blogs"
            ) final String blogsDirectoryPath
    ) {
        this.blogsDirectoryPath = blogsDirectoryPath;
    }

    /**
     * Renders the blog with the provided file name to HTML, caching the result
     * under the {@code blog-html} cache keyed by file name.
     *
     * @param fileName The blog file name to render.
     * @return The rendered HTML, or {@code null} if rendering failed.
     */
    @CacheResult(cacheName = "blog-html")
    public String render(@CacheKey final String fileName) {
        final File file = new File(blogsDirectoryPath + File.separator + fileName);

        return MarkdownParser.renderMarkdownToHtml(file);
    }

    /**
     * Evicts the cached HTML for the provided file name. Called by the blog
     * scheduler when a blog file is modified or deleted so stale HTML is not
     * served.
     *
     * @param fileName The blog file name whose cached HTML to evict.
     */
    @CacheInvalidate(cacheName = "blog-html")
    public void invalidate(@CacheKey final String fileName) {
        // TODO: Check if this is redundant
        // NOTE: Body intentionally empty; the interceptor evicts the entry.
    }
}
