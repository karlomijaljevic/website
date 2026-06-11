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

import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * <p>
 * A plain in-memory model that represents a blog. The blog cache is the single
 * source of truth; the <i>created</i> and <i>updated</i> values are derived
 * from the backing file's filesystem attributes and set by the scheduler that
 * reconciles the blogs directory.
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
     * Title of the blog. Derived from the first heading of the markdown file.
     */
    private String title;

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
     * Creation timestamp, derived from the backing file's creation time.
     */
    private LocalDateTime created;

    /**
     * Update timestamp, derived from the backing file's last-modified time.
     * Left {@code null} when it does not differ from {@link #created}.
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
