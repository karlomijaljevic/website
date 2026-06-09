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
package xyz.mijaljevic.domain.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.mijaljevic.domain.entity.Blog;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BlogServiceTest {
    @Mock
    EntityManager em;

    @Mock
    TypedQuery<Blog> query;

    private BlogService service;

    @BeforeEach
    void setUp() {
        service = new BlogService(em);
    }

    @Test
    @DisplayName("findBlogByFileName returns the single matching blog")
    void findBlogByFileName_match_returnsBlog() {
        Blog blog = new Blog();
        blog.setFileName("post.md");
        when(em.createQuery(anyString(), eq(Blog.class))).thenReturn(query);
        when(query.getSingleResult()).thenReturn(blog);

        assertThat(service.findBlogByFileName("post.md")).isSameAs(blog);
    }

    @Test
    @DisplayName("findBlogByFileName swallows NoResultException and returns null")
    void findBlogByFileName_noResult_returnsNull() {
        when(em.createQuery(anyString(), eq(Blog.class))).thenReturn(query);
        when(query.getSingleResult()).thenThrow(new NoResultException());

        assertThat(service.findBlogByFileName("missing.md")).isNull();
    }

    @Test
    @DisplayName("listAllBlogsMissingFromFileNames returns the query result list")
    void listAllBlogsMissingFromFileNames_returnsResultList() {
        Blog blog = new Blog();
        when(em.createQuery(anyString(), eq(Blog.class))).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(blog));

        assertThat(service.listAllBlogsMissingFromFileNames(List.of("kept.md")))
                .containsExactly(blog);
    }
}
