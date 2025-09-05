package xyz.mijaljevic.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.model.entity.StaticFile;
import xyz.mijaljevic.model.entity.StaticFile.Type;

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
        Query query = em.createQuery("delete from static_file where id = :id");

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
            List<String> fileNameList,
            Type type
    ) {
        TypedQuery<StaticFile> query = em.createQuery(
                "select SF from static_file SF where SF.type = :type and SF.name not in :fileNameList",
                StaticFile.class
        );

        query.setParameter("fileNameList", fileNameList);
        query.setParameter("type", type);

        return query.getResultList();
    }

    public StaticFile findFileByName(String name) {
        TypedQuery<StaticFile> query = em.createQuery(
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
