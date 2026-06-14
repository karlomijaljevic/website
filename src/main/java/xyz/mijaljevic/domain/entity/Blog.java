package xyz.mijaljevic.domain.entity;

import jakarta.annotation.Nonnull;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * <p>
 * A plain in-memory model that represents a blog. The blog cache is the single
 * source of truth; the <i>created</i> and <i>updated</i> values are derived
 * from the backing file's {@code Date}/{@code Updated} front-matter metadata
 * and set by the scheduler that reconciles the blogs' directory.
 * </p>
 *
 * <p>
 * Implements the {@link Comparable} interface where the natural ordering of
 * blogs is the newest goes first and the oldest goes last aka ordered by the
 * <i>created</i> attribute. This is against the standard recommendation
 * <i>(does not follow the equals() method)</i>. This is done to simplify the
 * sorting of blog models.
 * </p>
 */
@Data
public class Blog implements Comparable<Blog> {
    /**
     * Website date pattern used for displaying the created and updated dates.
     */
    private static final DateTimeFormatter WEBSITE_DATE_PATTERN = DateTimeFormatter.ofPattern("dd-MMM-uuuu");

    /**
     * Title of the blog. Derived from the {@code Title} front-matter metadata
     * tag, falling back to the first heading of the Markdown file.
     */
    private String title;

    /**
     * Author of the blog. Derived from the {@code Author} front-matter metadata
     * tag; {@code null} when the tag is absent.
     */
    private String author;

    /**
     * Tags of the blog. Derived from the {@code Tags} front-matter metadata
     * tag; empty when the tag is absent.
     */
    private List<String> tags = List.of();

    /**
     * URL slug derived from the blog title. Used as the public identifier in
     * {@code /blog/{slug}} URLs and RSS GUIDs.
     */
    private String slug;

    /**
     * Name of the source file backing the blog.
     */
    private String fileName;

    /**
     * Content hash used for HTTP <i>ETag</i> caching.
     */
    private String hash;

    /**
     * Creation timestamp, derived from the {@code Date} front-matter metadata
     * tag (at start of day). A blog without a parseable {@code Date} is
     * rejected, so this is never {@code null} for a cached blog.
     */
    private LocalDateTime created;

    /**
     * Update timestamp, derived from the {@code Updated} front-matter metadata
     * tag (at start of day). Left {@code null} when the tag is absent.
     */
    private LocalDateTime updated;

    /**
     * @return The formated <i>created</i> variable of the blog model.
     */
    public String parseCreated() {
        return WEBSITE_DATE_PATTERN.format(created);
    }

    /**
     * @return The formated <i>updated</i> variable of the blog model.
     */
    public String parseUpdated() {
        return updated == null ? "" : WEBSITE_DATE_PATTERN.format(updated);
    }

    @Override
    public int compareTo(@Nonnull final Blog other) {
        if (created.isBefore(other.created)) {
            return 1;
        } else if (created.isEqual(other.created)) {
            return 0;
        } else {
            return -1;
        }
    }
}
