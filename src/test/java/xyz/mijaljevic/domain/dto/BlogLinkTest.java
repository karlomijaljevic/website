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
package xyz.mijaljevic.domain.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.domain.entity.Blog;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class BlogLinkTest {
    @Test
    @DisplayName("generateBlogLinkFromBlog copies id and title and formats the created date")
    void generateBlogLinkFromBlog_mapsIdTitleAndFormattedDate() {
        Blog blog = new Blog();
        blog.setId(42L);
        blog.setTitle("My First Post");
        blog.setCreated(LocalDateTime.of(2026, 3, 5, 13, 30));

        BlogLink link = BlogLink.generateBlogLinkFromBlog(blog);

        assertThat(link.id()).isEqualTo(42L);
        assertThat(link.title()).isEqualTo("My First Post");
        // Blog.parseCreated() formats with the "dd-MMM-uuuu" pattern; the month
        // abbreviation is locale-dependent, so assert the structure (zero-padded
        // day, 3-char month, 4-digit year) rather than a fixed month string.
        assertThat(link.date()).matches("05-[A-Za-z]{3}-2026");
    }

    @Test
    @DisplayName("two BlogLinks with the same components are equal (record value semantics)")
    void blogLink_isValueEqual() {
        BlogLink a = new BlogLink(1L, "Title", "01-Jan-2026");
        BlogLink b = new BlogLink(1L, "Title", "01-Jan-2026");

        assertThat(a).isEqualTo(b);
        assertThat(a).hasSameHashCodeAs(b);
    }
}
