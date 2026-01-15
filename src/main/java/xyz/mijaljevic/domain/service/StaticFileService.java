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
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFile.Type;

import java.util.List;

/**
 * Service class used for {@link StaticFile} entities.
 */
@ApplicationScoped
public class StaticFileService {
    @Inject
    EntityManager em;

    @Transactional
    public void createStaticFile(StaticFile staticFile) {
        em.persist(staticFile);
    }

    @Transactional
    public StaticFile updateStaticFile(StaticFile staticFile) {
        return em.merge(staticFile);
    }

    @Transactional
    public boolean deleteStaticFile(StaticFile staticFile) {
        final Query query = em.createQuery("delete from static_file where id = :id");

        query.setParameter("id", staticFile.getId());

        return query.executeUpdate() == 1;
    }

    /**
     * Returns all <i>static</i> files of chosen type whose file names are not
     * contained in the provided file name list.
     *
     * @param fileNameList A list of <i>static</i> file names.
     * @param type         A {@link Type} which needs to be queried.
     * @return The {@link StaticFile} entities which do not match any file
     * names from the provided list of the provided type.
     */
    public List<StaticFile> listAllMissingFiles(
            final List<String> fileNameList,
            final Type type
    ) {
        final TypedQuery<StaticFile> query = em.createQuery(
                "select SF from static_file SF where SF.type = :type and SF.name not in :fileNameList",
                StaticFile.class
        );

        query.setParameter("fileNameList", fileNameList);
        query.setParameter("type", type);

        return query.getResultList();
    }

    public StaticFile findFileByName(String name) {
        final TypedQuery<StaticFile> query = em.createQuery(
                "select SF from static_file SF where SF.name = :name",
                StaticFile.class
        );

        query.setParameter("name", name);

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
