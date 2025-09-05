package xyz.mijaljevic.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * A model that represents a static file (images, css, etc.). The
 * <i>modified</i> value is automatically created/updated during entity
 * persistence/update there is no need to set it manually.
 */
@Entity(name = "static_file")
public class StaticFile {
    /**
     * Defines the static file types that the website serves. Currently, it
     * only servers (not including blog HTML pages):
     * <ul>
     *      <li>CSS</li>
     *      <li>IMAGE</li>
     * </ul>
     */
    public enum Type {
        CSS, IMAGE
    }

    @Id
    @SequenceGenerator(
            name = "staticFileSeq",
            sequenceName = "static_file_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "staticFileSeq")
    private Long id;

    @Column(
            name = "name",
            unique = true,
            nullable = false,
            updatable = false
    )
    private String name;

    @Column(
            name = "hash",
            nullable = false
    )
    private String hash;

    @Column(
            name = "modified",
            nullable = false
    )
    private LocalDateTime modified;

    @Column(
            name = "type",
            nullable = false
    )
    @Enumerated(EnumType.STRING)
    private Type type;

    @PrePersist
    void onCreate() {
        setModified(LocalDateTime.now());
    }

    @PreUpdate
    void onUpdate() {
        setModified(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(hash, id, modified, name, type);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        StaticFile other = (StaticFile) obj;

        return Objects.equals(hash, other.hash)
                && Objects.equals(id, other.id)
                && Objects.equals(modified, other.modified)
                && Objects.equals(name, other.name)
                && Objects.equals(type, other.type);
    }

    @Override
    public String toString() {
        return "StaticFile [id=" + id
                + ", hash=" + hash
                + ", name=" + name
                + ", modified=" + modified
                + ", type=" + type + "]";
    }
}
