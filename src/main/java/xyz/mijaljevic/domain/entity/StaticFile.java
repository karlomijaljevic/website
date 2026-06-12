package xyz.mijaljevic.domain.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * A plain in-memory model that represents a static file (images, CSS, etc.).
 * The <i>modified</i> value is derived from the backing file's last-modified
 * time and set by the scheduler that reconciles the static files' directory.
 */
@Data
public class StaticFile {
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
     * The {@link StaticFileType} of static file (CSS or image).
     */
    private StaticFileType type;
}
