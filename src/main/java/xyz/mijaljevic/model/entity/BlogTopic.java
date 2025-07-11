package xyz.mijaljevic.model.entity;

import jakarta.persistence.*;

import java.util.Objects;

/**
 * A model that represents a {@link Blog} and {@link Topic} combined key. This
 * entity exists because a single blog can have multiple topics and vice versa a
 * topic can have multiple blogs.
 */
@Entity(name = "blog_topic")
public class BlogTopic {
    @Id
    @SequenceGenerator(
            name = "blogTopicSeq",
            sequenceName = "blog_topic_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "blogTopicSeq")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "blog_id", referencedColumnName = "id", nullable = false)
    private Blog blog;

    @ManyToOne
    @JoinColumn(name = "topic_id", referencedColumnName = "id", nullable = false)
    private Topic topic;

    public BlogTopic() {
        setBlog(null);
        setTopic(null);
    }

    public BlogTopic(Blog blog, Topic topic) {
        setBlog(blog);
        setTopic(topic);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Blog getBlog() {
        return blog;
    }

    public void setBlog(Blog blog) {
        this.blog = blog;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    @Override
    public String toString() {
        return "BlogTopic [id=" + id + ", blog=" + blog + ", topic=" + topic + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(blog, id, topic);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        BlogTopic other = (BlogTopic) obj;
        return Objects.equals(blog, other.blog) && Objects.equals(id, other.id) && Objects.equals(topic, other.topic);
    }
}
