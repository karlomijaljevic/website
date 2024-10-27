package xyz.mijaljevic.orm;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.Transactional;
import xyz.mijaljevic.orm.model.Blog;

/**
 * Service class used for {@link Blog} entities.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@ApplicationScoped
public final class BlogService
{
	@Inject
	private EntityManager em;

	@Transactional
	public void createBlog(Blog blog)
	{
		em.persist(blog);
	}

	@Transactional
	public Blog updateBlog(Blog blog)
	{
		return em.merge(blog);
	}

	@Transactional
	public boolean deleteBlog(Blog blog)
	{
		Query query = em.createQuery("delete from blog where id = :id");

		query.setParameter("id", blog.getId());

		return query.executeUpdate() == 1;
	}

	public List<Blog> listAllBlogs()
	{
		TypedQuery<Blog> query = em.createQuery("select B from blog B", Blog.class);

		return query.getResultList();
	}

	/**
	 * Method will return all blogs whose file names are not contained in the
	 * provided list.
	 * 
	 * @param fileNameList A list of blog file names.
	 * 
	 * @return The {@link Blog} entities which do not match any file names from the
	 *         provided list.
	 */
	public List<Blog> listAllBlogsMissingFromFileNames(List<String> fileNameList)
	{
		TypedQuery<Blog> query = em.createQuery("select B from blog B where B.fileName not in :fileNameList",
				Blog.class);

		query.setParameter("fileNameList", fileNameList);

		return query.getResultList();
	}

	public Blog findBlogByFileName(String fileName)
	{
		TypedQuery<Blog> query = em.createQuery("select B from blog B where B.fileName = :fileName", Blog.class);

		query.setParameter("fileName", fileName);

		try
		{
			return query.getSingleResult();
		}
		catch (NoResultException e)
		{
			return null;
		}
	}
}
