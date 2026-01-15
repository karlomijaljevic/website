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

import lombok.Data;
import xyz.mijaljevic.domain.entity.Blog;

/**
 * A {@link Blog} link model. Used by the website to display blog links which
 * usually showcase only the blog title, created date and ID. This is a simple
 * POJO not an DB entity.
 */
@Data
public final class BlogLink {
    private Long id;
    private String title;
    private String date;

    /**
     * Generates a blog link instance from a blog instance.
     *
     * @param blog A {@link Blog} instance to create a blog link from.
     * @return A {@link BlogLink} instance from the blog.
     */
    public static BlogLink generateBlogLinkFromBlog(final Blog blog) {
        final BlogLink blogLink = new BlogLink();

        blogLink.setId(blog.getId());
        blogLink.setTitle(blog.getTitle());
        blogLink.setDate(blog.parseCreated());

        return blogLink;
    }
}
