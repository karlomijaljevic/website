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

package xyz.mijaljevic.domain.dto;

/**
 * An immutable carrier for a single RSS feed item, rendered by the
 * {@code rss.xml} Qute template. The {@code description} holds the
 * CommonMark-rendered blog HTML and is emitted raw inside a CDATA section by the
 * template; every other field is XML-escaped by Qute.
 *
 * @param title       The blog title.
 * @param link        The public {@code /blog/{slug}} URL of the blog.
 * @param guid        The globally unique identifier (same as {@code link}).
 * @param description The CommonMark-rendered blog HTML.
 * @param pubDate     The RFC-822 formatted publication date.
 */
public record RssItem(
        String title,
        String link,
        String guid,
        String description,
        String pubDate
) {
}
