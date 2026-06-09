/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */

package xyz.mijaljevic.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Transient;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
@Data
@Entity(name = "blog")
public class Blog implements Comparable<Blog> {
    /**
     * Website date pattern used for displaying the created and updated dates.
     */
    private static final DateTimeFormatter WEBSITE_DATE_PATTERN = DateTimeFormatter.ofPattern("dd-MMM-uuuu");

    /**
     * Primary key of the blog entity.
     */
    @Id
    @SequenceGenerator(
            name = "blogSeq",
            sequenceName = "blog_seq",
            allocationSize = 1
    )
    @GeneratedValue(generator = "blogSeq")
    private Long id;

    /**
     * Title of the blog. Unique and not nullable.
     */
    @Column(
            name = "title",
            nullable = false,
            unique = true
    )
    private String title;

    /**
     * Name of the source file backing the blog. Unique and immutable.
     */
    @Column(
            name = "file_name",
            nullable = false,
            unique = true,
            updatable = false
    )
    private String fileName;

    /**
     * Content hash used for HTTP <i>ETag</i> caching.
     */
    @Column(
            name = "hash",
            nullable = false
    )
    private String hash;

    /**
     * Timestamp set automatically when the entity is first persisted.
     */
    @Column(
            name = "created",
            nullable = false,
            updatable = false
    )
    private LocalDateTime created;

    /**
     * Timestamp set automatically whenever the entity is updated.
     */
    @Column(name = "updated")
    private LocalDateTime updated;

    /**
     * Transient timestamp of the last time the blog was read from cache.
     */
    @Transient
    private LocalDateTime lastRead;

    /**
     * Transient rendered HTML body of the blog, lazily populated.
     */
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

    @Override
    public int compareTo(final Blog other) {
        if (created.isBefore(other.created)) {
            return 1;
        } else if (created.isEqual(other.created)) {
            return 0;
        } else {
            return -1;
        }
    }
}
