package xyz.mijaljevic.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * <p>
 * A model that represents a blog. The update and created values are
 * automatically created/updated during entity persistence/update there is no
 * need to set them manually.
 * </p>
 *
 * <p>
 * Implements the {@link Comparable} interface where the natural ordering of
 * blogs is the newest goes first and the oldest goes last aka ordered by the
 * <i>created</i> attribute. This is against the standard recommendation
 * <i>(does not follow the equals() method)</i>. This is done to simplify the
 * sorting of blog entities.
 * </p>
 */
@Entity(name = "blog")
public class Blog implements Comparable<Blog> {
    /**
     * Website date pattern used for displaying the created and updated dates.
     */
    private static final DateTimeFormatter WEBSITE_DATE_PATTERN = DateTimeFormatter.ofPattern("dd-MMM-uuuu");

    @Id
    @SequenceGenerator(
            name = "blogSeq",
            sequenceName = "blog_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "blogSeq")
    private Long id;

    @Column(
            name = "title",
            nullable = false,
            unique = true
    )
    private String title;

    @Column(
            name = "file_name",
            nullable = false,
            unique = true,
            updatable = false
    )
    private String fileName;

    @Column(
            name = "hash",
            nullable = false
    )
    private String hash;

    @Column(
            name = "created",
            nullable = false,
            updatable = false
    )
    private LocalDateTime created;

    @Column(name = "updated")
    private LocalDateTime updated;

    @Transient
    private LocalDateTime lastRead;

    @Transient
    private String data;

    @PrePersist
    void onCreate() {
        setCreated(LocalDateTime.now());
    }

    @PreUpdate
    void onUpdate() {
        setUpdated(LocalDateTime.now());
    }

    /**
     * @return The formated <i>created</i> variable of the blog entity.
     */
    public String parseCreated() {
        return WEBSITE_DATE_PATTERN.format(created);
    }

    /**
     * @return The formated <i>updated</i> variable of the blog entity.
     */
    public String parseUpdated() {
        return updated == null ? "" : WEBSITE_DATE_PATTERN.format(updated);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public void setUpdated(LocalDateTime updated) {
        this.updated = updated;
    }

    public LocalDateTime getLastRead() {
        return lastRead;
    }

    public void setLastRead(LocalDateTime lastRead) {
        this.lastRead = lastRead;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public int compareTo(Blog other) {
        if (created.isBefore(other.created)) {
            return 1;
        } else if (created.isEqual(other.created)) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(created, fileName, id, title, updated, hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        Blog other = (Blog) obj;

        return Objects.equals(created, other.created)
                && Objects.equals(fileName, other.fileName)
                && Objects.equals(id, other.id)
                && Objects.equals(title, other.title)
                && Objects.equals(updated, other.updated)
                && Objects.equals(hash, other.hash);
    }

    @Override
    public String toString() {
        return "Blog [id=" + id
                + ", title=" + title
                + ", fileName=" + fileName
                + ", created=" + created
                + ", updated=" + updated
                + ", hash=" + hash + "]";
    }
}
