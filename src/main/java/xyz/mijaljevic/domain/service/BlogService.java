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

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.domain.entity.Blog;

import java.util.List;

/**
 * Service class used for {@link Blog} entities.
 */
@ApplicationScoped
public final class BlogService {
    /**
     * JPA entity manager used for {@link Blog} persistence operations.
     */
    private final EntityManager em;

    /**
     * Creates the service with the injected {@link EntityManager}.
     *
     * @param em The JPA {@link EntityManager}.
     */
    @Inject
    public BlogService(final EntityManager em) {
        this.em = em;
    }

    /**
     * Persists a new blog entity.
     *
     * @param blog The {@link Blog} to persist.
     */
    @Transactional
    public void createBlog(final Blog blog) {
        em.persist(blog);
    }

    /**
     * Merges an existing blog entity.
     *
     * @param blog The {@link Blog} to update.
     * @return The merged {@link Blog} instance.
     */
    @Transactional
    public Blog updateBlog(final Blog blog) {
        return em.merge(blog);
    }

    /**
     * Deletes the blog entity matching the provided blog's ID.
     *
     * @param blog The {@link Blog} to delete.
     * @return {@code true} if exactly one row was deleted.
     */
    @Transactional
    public boolean deleteBlog(final Blog blog) {
        final Query query = em.createQuery("delete from blog where id = :id");

        query.setParameter("id", blog.getId());

        return query.executeUpdate() == 1;
    }

    /**
     * Method will return all blogs whose file names are not contained in the
     * provided list.
     *
     * @param fileNameList A list of blog file names.
     * @return The {@link Blog} entities which do not match any file names from
     * the provided list.
     */
    public List<Blog> listAllBlogsMissingFromFileNames(
            final List<String> fileNameList
    ) {
        final TypedQuery<Blog> query = em.createQuery(
                "select B from blog B where B.fileName not in :fileNameList",
                Blog.class
        );

        query.setParameter("fileNameList", fileNameList);

        return query.getResultList();
    }

    /**
     * Finds a blog by its file name.
     *
     * @param fileName The file name to search for.
     * @return The matching {@link Blog}, or {@code null} if none exists.
     */
    public Blog findBlogByFileName(final String fileName) {
        final TypedQuery<Blog> query = em.createQuery(
                "select B from blog B where B.fileName = :fileName",
                Blog.class
        );

        query.setParameter("fileName", fileName);

        try {
            return query.getSingleResult();
        } catch (NoResultException noResultException) {
            // NOTE: No blog matches the file name; absence is a normal result.
            return null;
        }
    }
}
