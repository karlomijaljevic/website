package xyz.mijaljevic.domain.dto;

import jakarta.annotation.Nonnull;
import xyz.mijaljevic.domain.entity.Blog;

/**
 * A {@link Blog} link model. Used by the website to display blog links which
 * usually showcase only the blog title, created date and slug. This is a simple
 * immutable carrier.
 *
 * @param slug  The blog slug, used as the public identifier in URLs.
 * @param title The blog title.
 * @param date  The formatted blog creation date.
 */
public record BlogLink(String slug, String title, String date) {
    /**
     * Generates a blog link instance from a blog instance.
     *
     * @param blog A {@link Blog} instance to create a blog link from.
     * @return A {@link BlogLink} instance from the blog.
     */
    @Nonnull
    public static BlogLink generateBlogLinkFromBlog(final Blog blog) {
        return new BlogLink(blog.getSlug(), blog.getTitle(), blog.parseCreated());
    }
}
