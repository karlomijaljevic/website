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
    @Inject
    EntityManager em;

    @Transactional
    public void createBlog(Blog blog) {
        em.persist(blog);
    }

    @Transactional
    public Blog updateBlog(Blog blog) {
        return em.merge(blog);
    }

    @Transactional
    public boolean deleteBlog(Blog blog) {
        Query query = em.createQuery("delete from blog where id = :id");

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
            List<String> fileNameList
    ) {
        TypedQuery<Blog> query = em.createQuery(
                "select B from blog B where B.fileName not in :fileNameList",
                Blog.class
        );

        query.setParameter("fileNameList", fileNameList);

        return query.getResultList();
    }

    public Blog findBlogByFileName(String fileName) {
        TypedQuery<Blog> query = em.createQuery(
                "select B from blog B where B.fileName = :fileName",
                Blog.class
        );

        query.setParameter("fileName", fileName);

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
