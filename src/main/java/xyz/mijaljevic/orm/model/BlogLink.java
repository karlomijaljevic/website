package xyz.mijaljevic.orm.model;

import java.util.Objects;

/**
 * A {@link Blog} link model. Used by the website to display blog links which
 * usually showcase only the blog title, created date and ID. This is a simple
 * POJO not an DB entity.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
public final class BlogLink
{
	private Long id;
	private String title;
	private String date;

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
