package xyz.mijaljevic.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.model.entity.Topic;

/**
 * Service class used for {@link Topic} entities.
 */
@ApplicationScoped
public final class TopicService {
    /**
     * The entity manager used to interact with the database.
     */
    @Inject
    EntityManager em;

    /**
     * Finds a topic by its name.
     *
     * @param name the name of the topic to find
     * @return the found topic, or null if no topic with that name exists
     */
    public Topic findTopicByName(String name) {
        TypedQuery<Topic> query = em.createQuery(
                "select T from topic T where T.name = :name",
                Topic.class
        ).setParameter("name", name);

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    /**
     * Creates a new topic.
     *
     * @param topic the topic to create
     * @return the created topic, or null if it failed
     */
    @Transactional
    public Topic createTopic(Topic topic) {
        em.persist(topic);
        return findTopicByName(topic.getName());
    }

    /**
     * Updates an existing topic.
     *
     * @param topic the topic to update
     * @return the updated topic
     */
    @Transactional
    public Topic updateTopic(Topic topic) {
        return em.merge(topic);
    }
}
