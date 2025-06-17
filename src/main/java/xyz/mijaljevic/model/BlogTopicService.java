package xyz.mijaljevic.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.model.entity.BlogTopic;

/**
 * Service class used for {@link BlogTopic} entities.
 */
@ApplicationScoped
public class BlogTopicService {
    /**
     * Entity manager used for database operations. Injected by CDI.
     */
    @Inject
    EntityManager em;

    /**
     * Creates a new blog topic in the database.
     *
     * @param blogTopic {@link BlogTopic} entity to be created
     */
    @Transactional
    public void createBlogTopic(BlogTopic blogTopic) {
        em.persist(blogTopic);
    }

    /**
     * Returns all blog topics for the specified blog and topic ID.
     *
     * @param blogId  Blog ID
     * @param topicId Topic ID
     * @return {@link BlogTopic} with the specified blog and topic ID, or null
     * if not found.
     */
    @Transactional
    public BlogTopic findBlogTopicByBlogAndTopicId(Long blogId, Long topicId) {
        if (blogId == null || topicId == null) {
            throw new IllegalArgumentException("Blog ID and Topic ID must not be null");
        }

        if (blogId <= 0 || topicId <= 0) {
            throw new IllegalArgumentException("Blog ID and Topic ID must be greater than zero");
        }

        try {
            return em.createQuery(
                            "SELECT bt FROM blog_topic bt WHERE bt.blog.id = :blogId AND bt.topic.id = :topicId",
                            BlogTopic.class
                    )
                    .setParameter("blogId", blogId)
                    .setParameter("topicId", topicId)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
