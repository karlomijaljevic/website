package xyz.mijaljevic.model.dto;

import java.util.Objects;

import xyz.mijaljevic.model.entity.Blog;

/**
 * A {@link Blog} link model. Used by the website to display blog links which
 * usually show case only the blog title, created date and ID. This is a simple
 * POJO not an DB entity.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0
 */
public final class BlogLink
{
	private Long id;
	private String title;
	private String date;

	/**
	 * Generates a blog link instance from a blog instance.
	 * 
	 * @param blog A {@link Blog} instance to create a blog link from.
	 * 
	 * @return A {@link BlogLink} instance from the blog.
	 */
	public final static BlogLink generateBlogLinkFromBlog(Blog blog)
	{
		BlogLink blogLink = new BlogLink();

		blogLink.setId(blog.getId());
		blogLink.setTitle(blog.getTitle());
		blogLink.setDate(blog.parseCreated());

		return blogLink;
	}

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getTitle()
	{
		return title;
	}

	public void setTitle(String title)
	{
		this.title = title;
	}

	public String getDate()
	{
		return date;
	}

	public void setDate(String date)
	{
		this.date = date;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(date, id, title);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		BlogLink other = (BlogLink) obj;
		return Objects.equals(date, other.date) && Objects.equals(id, other.id) && Objects.equals(title, other.title);
	}

	@Override
	public String toString()
	{
		return "BlogLink [id=" + id + ", title=" + title + ", date=" + date + "]";
	}
}
