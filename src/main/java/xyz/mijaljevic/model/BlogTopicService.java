package xyz.mijaljevic.model;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.model.entity.BlogTopic;

/**
 * Service class used for {@link BlogTopic} entities.
 */
@ApplicationScoped
public class BlogTopicService {
    @Inject
    EntityManager em;

    @Transactional
    public void createBlogTopic(BlogTopic blogTopic) {
        em.persist(blogTopic);
    }
}
