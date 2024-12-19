package xyz.mijaljevic.model.entity;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;

/**
 * A model that represents a topic.
 * 
 * @author karlo
 * 
 * @since 12.2024
 * 
 * @version 1.0
 */
@Entity(name = "topic")
public class Topic
{
	@Id
	@SequenceGenerator(name = "topicSeq", sequenceName = "topic_seq", allocationSize = 1, initialValue = 1)
	@GeneratedValue(generator = "topicSeq")
	private Long id;

	@Column(name = "name", nullable = false, unique = true, updatable = false)
	private String name;

	@OneToMany(mappedBy = "topic")
	private List<BlogTopic> blogTopics;

	public Long getId()
	{
		return id;
	}

	public void setId(Long id)
	{
		this.id = id;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public List<BlogTopic> getBlogTopics()
	{
		return blogTopics;
	}

	public void setBlogTopics(List<BlogTopic> blogTopics)
	{
		this.blogTopics = blogTopics;
	}

	@Override
	public String toString()
	{
		return "Topic [id=" + id + ", name=" + name + "]";
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id, name);
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		Topic other = (Topic) obj;
		return Objects.equals(id, other.id) && Objects.equals(name, other.name);
	}
}
