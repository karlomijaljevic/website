package xyz.mijaljevic.scheduler;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.cache.BlogCache;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.test.BlogsDirectoryTestResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for the blog scheduler's startup reconcile: the
 * {@code @PostConstruct} pass must populate the {@link BlogCache} from the
 * seeded blogs directory, indexing every file by slug and deriving the
 * timestamps from each file's {@code Date}/{@code Updated} front-matter
 * metadata.
 */
@QuarkusTest
@QuarkusTestResource(value = BlogsDirectoryTestResource.class, restrictToAnnotatedClass = true)
class BlogSchedulerReconcileTest {
    /**
     * The application blog cache, reconciled from the seeded directory by the
     * {@code @Startup} scheduler's {@code @PostConstruct} before any test runs.
     */
    @Inject
    BlogCache blogCache;

    @Test
    @DisplayName("Startup reconcile indexes every seeded blog by slug")
    void startupReconcile_indexesSeededBlogsBySlug() {
        Blog alpha = blogCache.bySlug(BlogsDirectoryTestResource.ALPHA_SLUG);
        Blog beta = blogCache.bySlug(BlogsDirectoryTestResource.BETA_SLUG);
        Blog gamma = blogCache.bySlug(BlogsDirectoryTestResource.GAMMA_SLUG);

        assertThat(alpha).isNotNull();
        assertThat(beta).isNotNull();
        assertThat(gamma).isNotNull();

        assertThat(alpha.getTitle()).isEqualTo(BlogsDirectoryTestResource.ALPHA_TITLE);
        assertThat(alpha.getFileName()).isEqualTo(BlogsDirectoryTestResource.ALPHA_FILE);
        assertThat(alpha.getHash()).isNotBlank();
        // Created, title, author and tags are all driven by alpha's metadata.
        assertThat(alpha.getCreated()).isNotNull();
    }

    @Test
    @DisplayName("Front-matter metadata drives a blog's title, author, tags and created date")
    void startupReconcile_metadataDrivesBlogFields() {
        Blog alpha = blogCache.bySlug(BlogsDirectoryTestResource.ALPHA_SLUG);
        Blog beta = blogCache.bySlug(BlogsDirectoryTestResource.BETA_SLUG);

        assertThat(alpha).isNotNull();
        assertThat(beta).isNotNull();

        // Alpha carries metadata: title, author, tags, created and updated all
        // come from the front-matter block rather than the heading.
        assertThat(alpha.getTitle()).isEqualTo(BlogsDirectoryTestResource.ALPHA_TITLE);
        assertThat(alpha.getAuthor()).isEqualTo(BlogsDirectoryTestResource.ALPHA_AUTHOR);
        assertThat(alpha.getTags()).containsExactly("alpha", "test");
        assertThat(alpha.getCreated()).isEqualTo(BlogsDirectoryTestResource.ALPHA_CREATED);
        assertThat(alpha.getUpdated()).isEqualTo(BlogsDirectoryTestResource.ALPHA_UPDATED);

        // Beta carries only a Date: title falls back to the heading, author/tags
        // stay empty, created comes from the metadata, updated stays null.
        assertThat(beta.getTitle()).isEqualTo(BlogsDirectoryTestResource.BETA_TITLE);
        assertThat(beta.getAuthor()).isNull();
        assertThat(beta.getTags()).isEmpty();
        assertThat(beta.getCreated()).isEqualTo(BlogsDirectoryTestResource.BETA_CREATED);
        assertThat(beta.getUpdated()).isNull();
    }

    @Test
    @DisplayName("Startup reconcile exposes every seeded blog through recent() and all()")
    void startupReconcile_exposesSeededBlogs() {
        List<Blog> recent = blogCache.recent();
        List<Blog> all = blogCache.all();

        assertThat(recent).hasSize(BlogsDirectoryTestResource.SEEDED_BLOG_COUNT);
        assertThat(all).hasSize(BlogsDirectoryTestResource.SEEDED_BLOG_COUNT);

        assertThat(all)
                .extracting(Blog::getSlug)
                .containsExactlyInAnyOrder(
                        BlogsDirectoryTestResource.ALPHA_SLUG,
                        BlogsDirectoryTestResource.BETA_SLUG,
                        BlogsDirectoryTestResource.GAMMA_SLUG
                );
    }

    @Test
    @DisplayName("Reconciled blogs are reachable by file name as well as by slug")
    void startupReconcile_indexesByFileName() {
        Blog byFile = blogCache.byFileName(BlogsDirectoryTestResource.BETA_FILE);

        assertThat(byFile).isNotNull();
        assertThat(byFile.getSlug()).isEqualTo(BlogsDirectoryTestResource.BETA_SLUG);
    }
}
