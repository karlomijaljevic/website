package xyz.mijaljevic.model;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.model.entity.Topic;

/**
 * Service class used for {@link Topic} entities.
 */
@ApplicationScoped
public final class TopicService
{
	@Inject
	private EntityManager em;

	@Transactional
	public void createTopic(Topic topic)
	{
		em.persist(topic);
	}

	@Transactional
	public Topic updateTopic(Topic topic)
	{
		return em.merge(topic);
	}

	@Transactional
	public boolean deleteTopic(Topic topic)
	{
		Query query = em.createQuery("delete from topic where id = :id");

		query.setParameter("id", topic.getId());

		return query.executeUpdate() == 1;
	}

	public List<Topic> listAllTopics()
	{
		TypedQuery<Topic> query = em.createQuery("select T from topic T", Topic.class);

		return query.getResultList();
	}
}
