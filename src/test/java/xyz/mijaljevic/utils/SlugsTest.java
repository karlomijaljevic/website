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
package xyz.mijaljevic.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SlugsTest {
    @Test
    @DisplayName("slugify lowercases and hyphenates a simple title")
    void slugify_simpleTitle() {
        assertThat(Slugs.slugify("My First Post")).isEqualTo("my-first-post");
    }

    @Test
    @DisplayName("slugify collapses runs of punctuation and whitespace into single hyphens")
    void slugify_collapsesSeparators() {
        assertThat(Slugs.slugify("Hello,   World!! -- Again")).isEqualTo("hello-world-again");
    }

    @Test
    @DisplayName("slugify trims leading and trailing hyphens")
    void slugify_trimsEdgeHyphens() {
        assertThat(Slugs.slugify("  ...Edges...  ")).isEqualTo("edges");
    }

    @Test
    @DisplayName("slugify strips diacritics from accented characters")
    void slugify_stripsDiacritics() {
        assertThat(Slugs.slugify("Čišćenje žabe")).isEqualTo("ciscenje-zabe");
    }

    @Test
    @DisplayName("slugify of a title without alphanumerics yields an empty slug")
    void slugify_noAlphanumerics_returnsEmpty() {
        assertThat(Slugs.slugify("--- !!! ---")).isEmpty();
    }

    @Test
    @DisplayName("identical titles produce identical slugs (collision source)")
    void slugify_identicalTitles_collide() {
        assertThat(Slugs.slugify("Same Title")).isEqualTo(Slugs.slugify("same title"));
    }

    @Test
    @DisplayName("slugify rejects a null title")
    void slugify_null_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> Slugs.slugify(null));
    }

    @Test
    @DisplayName("slugify keeps digits and mixes them with words")
    void slugify_keepsDigits() {
        assertThat(Slugs.slugify("Top 10 Tips for 2025")).isEqualTo("top-10-tips-for-2025");
    }

    @Test
    @DisplayName("slugify of an already-slug-shaped title is idempotent")
    void slugify_idempotentOnSlug() {
        assertThat(Slugs.slugify("already-a-slug")).isEqualTo("already-a-slug");
    }
}
