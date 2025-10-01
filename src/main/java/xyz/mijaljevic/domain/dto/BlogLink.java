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

import xyz.mijaljevic.domain.entity.Blog;

import java.util.Objects;

/**
 * A {@link Blog} link model. Used by the website to display blog links which
 * usually showcase only the blog title, created date and ID. This is a simple
 * POJO not an DB entity.
 */
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
    public static BlogLink generateBlogLinkFromBlog(Blog blog) {
        BlogLink blogLink = new BlogLink();

        blogLink.setId(blog.getId());
        blogLink.setTitle(blog.getTitle());
        blogLink.setDate(blog.parseCreated());

        return blogLink;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    @Override
    public int hashCode() {
        return Objects.hash(date, id, title);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        BlogLink other = (BlogLink) obj;

        return Objects.equals(date, other.date)
                && Objects.equals(id, other.id)
                && Objects.equals(title, other.title);
    }

    @Override
    public String toString() {
        return "BlogLink [id=" + id + ", title=" + title + ", date=" + date + "]";
    }
}
