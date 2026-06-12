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
