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

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * A model that represents a static file (images, css, etc.). The
 * <i>modified</i> value is automatically created/updated during entity
 * persistence/update there is no need to set it manually.
 */
@Data
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
}
