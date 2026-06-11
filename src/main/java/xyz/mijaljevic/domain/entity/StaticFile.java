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

/**
 * A plain in-memory model that represents a static file (images, css, etc.).
 * The <i>modified</i> value is derived from the backing file's last-modified
 * time and set by the scheduler that reconciles the static files directory.
 */
@Data
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
        /**
         * A CSS stylesheet.
         */
        CSS,
        /**
         * An image file.
         */
        IMAGE
    }

    /**
     * Name of the static file.
     */
    private String name;

    /**
     * Content hash used for HTTP <i>ETag</i> caching.
     */
    private String hash;

    /**
     * Last-modified timestamp, derived from the backing file.
     */
    private LocalDateTime modified;

    /**
     * The {@link Type} of static file (CSS or image).
     */
    private Type type;
}
