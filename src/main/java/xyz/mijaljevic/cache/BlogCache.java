package xyz.mijaljevic.cache;

import io.quarkus.logging.Log;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.entity.Blog;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application scoped in-memory index of {@link Blog} models; this cache
 * is the single source of truth for blogs.
 *
 * <p>
 * Maintains two consistent indexes: a primary one keyed by file name (the unit
 * the scheduler works in) and a secondary one keyed by slug (the unit requests
 * look up), so blog lookups are O(1) instead of an O(n) scan.
 * </p>
 */
@ApplicationScoped
public class BlogCache {
    /**
     * Primary index keyed by blog file name.
     */
    private final Map<String, Blog> byFileName = new ConcurrentHashMap<>();

    /**
     * Secondary index keyed by blog slug.
     */
    private final Map<String, Blog> bySlug = new ConcurrentHashMap<>();

    /**
     * Inserts or updates the provided blog in both indexes, keeping them
     * consistent. If the blog's title (and therefore slug) changed, the stale
     * slug mapping is removed first. On a slug collision with a different file
     * the first blog is kept and a warning is logged.
     *
     * @param blog The {@link Blog} to store.
     */
    public void put(@Nonnull final Blog blog) {
        final Blog previous = byFileName.get(blog.getFileName());

        if (previous != null && !previous.getSlug().equals(blog.getSlug())) {
            bySlug.remove(previous.getSlug(), previous);
        }

        byFileName.put(blog.getFileName(), blog);

        final Blog slugOwner = bySlug.get(blog.getSlug());

        if (slugOwner == null || slugOwner.getFileName().equals(blog.getFileName())) {
            bySlug.put(blog.getSlug(), blog);
        } else {
            Log.warnf(
                    "Slug collision: '%s' already used by file '%s', ignoring file '%s'",
                    blog.getSlug(),
                    slugOwner.getFileName(),
                    blog.getFileName()
            );
        }
    }

    /**
     * Removes the blog with the provided file name from both indexes. If the
     * removed blog owned its slug mapping, the slug is reassigned to another
     * remaining blog with the same slug, if any.
     *
     * @param fileName The file name of the blog to remove.
     */
    public void removeByFileName(final String fileName) {
        final Blog removed = byFileName.remove(fileName);

        if (removed == null) {
            return;
        }

        if (bySlug.remove(removed.getSlug(), removed)) {
            byFileName.values()
                    .stream()
                    .filter(blog -> blog.getSlug().equals(removed.getSlug()))
                    .findFirst()
                    .ifPresent(blog -> bySlug.put(blog.getSlug(), blog));
        }
    }

    /**
     * Finds a blog by its slug.
     *
     * @param slug The slug to search for.
     * @return The matching {@link Blog}, or {@code null} if none exists.
     */
    public Blog bySlug(final String slug) {
        return bySlug.get(slug);
    }

    /**
     * Finds a blog by its file name.
     *
     * @param fileName The file name to search for.
     * @return The matching {@link Blog}, or {@code null} if none exists.
     */
    public Blog byFileName(final String fileName) {
        return byFileName.get(fileName);
    }

    /**
     * @return The most recent blogs (ordered newest first), limited to
     * {@link Website#NUMBER_OF_BLOGS_TO_DISPLAY}.
     */
    public List<Blog> recent() {
        return byFileName.values()
                .stream()
                .sorted()
                .limit(Website.NUMBER_OF_BLOGS_TO_DISPLAY)
                .toList();
    }

    /**
     * @return All cached blogs, ordered newest first.
     */
    public List<Blog> all() {
        return byFileName.values()
                .stream()
                .sorted()
                .toList();
    }

    /**
     * Returns all cached blogs whose file names are not contained in the
     * provided collection. Used by the scheduler to detect blogs whose backing
     * file was removed.
     *
     * @param fileNames The file names that currently exist on disk.
     * @return The cached {@link Blog} models missing from the provided
     * collection.
     */
    public List<Blog> missing(final Collection<String> fileNames) {
        return byFileName.values()
                .stream()
                .filter(blog -> !fileNames.contains(blog.getFileName()))
                .toList();
    }
}
