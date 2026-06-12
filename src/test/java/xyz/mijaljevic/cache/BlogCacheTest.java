package xyz.mijaljevic.cache;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.utils.Slugs;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link BlogCache}: the create / update / delete semantics, the
 * dual file-name and slug indexes staying consistent across title changes and
 * slug collisions, and the ordering and limit guarantees of {@code recent()}.
 */
class BlogCacheTest {
    @Test
    @DisplayName("put indexes a blog by both file name and slug")
    void put_indexesByFileNameAndSlug() {
        BlogCache cache = new BlogCache();
        Blog blog = blog("First Post", "first.md", baseTime());

        cache.put(blog);

        assertThat(cache.byFileName("first.md")).isSameAs(blog);
        assertThat(cache.bySlug("first-post")).isSameAs(blog);
    }

    @Test
    @DisplayName("a missing slug or file name returns null")
    void lookup_missing_returnsNull() {
        BlogCache cache = new BlogCache();

        assertThat(cache.bySlug("nope")).isNull();
        assertThat(cache.byFileName("nope.md")).isNull();
    }

    @Test
    @DisplayName("re-putting the same file with a new title re-indexes the slug and drops the stale one")
    void put_titleChange_reindexesSlug() {
        BlogCache cache = new BlogCache();
        cache.put(blog("Old Title", "post.md", baseTime()));

        cache.put(blog("New Title", "post.md", baseTime()));

        assertThat(cache.bySlug("old-title")).isNull();
        assertThat(cache.bySlug("new-title")).isNotNull();
        assertThat(cache.byFileName("post.md").getTitle()).isEqualTo("New Title");
    }

    @Test
    @DisplayName("removeByFileName drops the blog from both indexes")
    void removeByFileName_dropsFromBothIndexes() {
        BlogCache cache = new BlogCache();
        cache.put(blog("Doomed Post", "doomed.md", baseTime()));

        cache.removeByFileName("doomed.md");

        assertThat(cache.byFileName("doomed.md")).isNull();
        assertThat(cache.bySlug("doomed-post")).isNull();
    }

    @Test
    @DisplayName("removeByFileName for an unknown file is a no-op")
    void removeByFileName_unknown_isNoOp() {
        BlogCache cache = new BlogCache();
        cache.put(blog("Kept Post", "kept.md", baseTime()));

        cache.removeByFileName("ghost.md");

        assertThat(cache.byFileName("kept.md")).isNotNull();
    }

    @Test
    @DisplayName("on a slug collision the first file wins and the second is ignored on the slug index")
    void put_slugCollision_keepsFirstOwner() {
        BlogCache cache = new BlogCache();
        Blog first = blog("Same Title", "one.md", baseTime());
        Blog second = blog("Same Title", "two.md", baseTime());

        cache.put(first);
        cache.put(second);

        // Both files are still indexed primarily, but the slug keeps the first.
        assertThat(cache.byFileName("one.md")).isSameAs(first);
        assertThat(cache.byFileName("two.md")).isSameAs(second);
        assertThat(cache.bySlug("same-title")).isSameAs(first);
    }

    @Test
    @DisplayName("recent orders blogs newest first and caps at the display limit")
    void recent_ordersNewestFirstAndCaps() {
        BlogCache cache = new BlogCache();
        int total = Website.NUMBER_OF_BLOGS_TO_DISPLAY + 4;

        // Index oldest to newest so ordering is exercised, not accidental.
        for (int i = 0; i < total; i++) {
            cache.put(blog("Post " + i, "post-" + i + ".md", baseTime().plusDays(i)));
        }

        List<Blog> recent = cache.recent();

        assertThat(recent).hasSize(Website.NUMBER_OF_BLOGS_TO_DISPLAY);
        // The newest blog (highest offset) must be first.
        assertThat(recent.get(0).getFileName())
                .isEqualTo("post-" + (total - 1) + ".md");
        assertThat(recent).isSortedAccordingTo(Blog::compareTo);
    }

    @Test
    @DisplayName("all returns every blog ordered newest first")
    void all_returnsEveryBlogSorted() {
        BlogCache cache = new BlogCache();
        cache.put(blog("Oldest", "old.md", baseTime()));
        cache.put(blog("Newest", "new.md", baseTime().plusDays(1)));

        List<Blog> all = cache.all();

        assertThat(all).hasSize(2);
        assertThat(all.get(0).getTitle()).isEqualTo("Newest");
        assertThat(all.get(1).getTitle()).isEqualTo("Oldest");
    }

    @Test
    @DisplayName("missing returns the cached blogs whose file is absent from the provided set")
    void missing_returnsAbsentBackedBlogs() {
        BlogCache cache = new BlogCache();
        cache.put(blog("Kept", "kept.md", baseTime()));
        cache.put(blog("Gone", "gone.md", baseTime()));

        List<Blog> missing = cache.missing(List.of("kept.md"));

        assertThat(missing)
                .extracting(Blog::getFileName)
                .containsExactly("gone.md");
    }

    /**
     * @return A fixed base creation timestamp shared by the test blogs.
     */
    private static LocalDateTime baseTime() {
        return LocalDateTime.of(2025, 1, 1, 12, 0, 0);
    }

    /**
     * Builds a {@link Blog} the way the scheduler would, deriving its slug from
     * the title.
     *
     * @param title    The blog title.
     * @param fileName The backing file name.
     * @param created  The creation timestamp.
     * @return The populated {@link Blog}.
     */
    private static Blog blog(final String title, final String fileName, final LocalDateTime created) {
        Blog blog = new Blog();
        blog.setTitle(title);
        blog.setSlug(Slugs.slugify(title));
        blog.setFileName(fileName);
        blog.setHash("hash-" + fileName);
        blog.setCreated(created);
        return blog;
    }
}
