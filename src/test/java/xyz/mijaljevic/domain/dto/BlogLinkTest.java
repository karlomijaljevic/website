package xyz.mijaljevic.domain.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.domain.entity.Blog;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BlogLinkTest {
    @Test
    @DisplayName("generateBlogLinkFromBlog copies slug and title and formats the created date")
    void generateBlogLinkFromBlog_mapsSlugTitleAndFormattedDate() {
        Blog blog = new Blog();
        blog.setSlug("my-first-post");
        blog.setTitle("My First Post");
        blog.setCreated(LocalDateTime.of(2026, 3, 5, 13, 30));

        BlogLink link = BlogLink.generateBlogLinkFromBlog(blog);

        assertThat(link.slug()).isEqualTo("my-first-post");
        assertThat(link.title()).isEqualTo("My First Post");
        // Blog.parseCreated() formats with the "dd-MMM-uuuu" pattern; the month
        // abbreviation is locale-dependent, so assert the structure (zero-padded
        // day, 3-char month, 4-digit year) rather than a fixed month string.
        assertThat(link.date()).matches("05-[A-Za-z]{3}-2026");
    }

    @Test
    @DisplayName("two BlogLinks with the same components are equal (record value semantics)")
    void blogLink_isValueEqual() {
        BlogLink a = new BlogLink("title", "Title", "01-Jan-2026");
        BlogLink b = new BlogLink("title", "Title", "01-Jan-2026");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
