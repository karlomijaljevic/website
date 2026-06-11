/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */

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
 * timestamps from the backing file's filesystem attributes.
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
        // Timestamps are derived from the filesystem, never left null on created.
        assertThat(alpha.getCreated()).isNotNull();
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
