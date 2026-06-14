package xyz.mijaljevic.domain.dto;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * Immutable carrier for the Markdown front-matter metadata block that prefixes
 * a blog file. The block is delimited by {@code ---} fences and holds simple
 * {@code Key: value} pairs, e.g.
 *
 * <pre>
 * ---
 * Title:    Best blog ever
 * Author:   Donald Trump
 * Date:     27-Jan-2026
 * Updated:  29-Jan-2026
 * Tags:     donald, metadata, tags, markdown
 * ---
 * </pre>
 *
 * <p>
 * Individual fields are {@code null} (or, for {@link #tags()}, empty) when the
 * corresponding key is absent or unparseable. The {@code Title} title falls
 * back to the first heading when absent; the {@code Date} is the sole source of
 * the blog's created timestamp (there is no filesystem fallback).
 * </p>
 *
 * @param title   The blog title, or {@code null} when absent.
 * @param author  The blog author, or {@code null} when absent.
 * @param date    The blog publication date, or {@code null} when absent or
 *                unparseable.
 * @param updated The blog last-updated date, or {@code null} when absent or
 *                unparseable.
 * @param tags    The blog tags; never {@code null}, empty when absent.
 */
public record BlogMetadata(
        @Nullable String title,
        @Nullable String author,
        @Nullable LocalDate date,
        @Nullable LocalDate updated,
        @Nonnull List<String> tags
) {
    /**
     * Shared instance representing the absence of any metadata.
     */
    public static final BlogMetadata EMPTY = new BlogMetadata(null, null, null, null, List.of());

    /**
     * Canonical constructor. Defensively copies the tags list and substitutes
     * an empty list for {@code null}, keeping the record immutable and its tags
     * accessor null-safe.
     */
    public BlogMetadata {
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
}
