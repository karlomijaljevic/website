package xyz.mijaljevic.web;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.cache.BlogCache;
import xyz.mijaljevic.cache.BlogRenderer;
import xyz.mijaljevic.domain.dto.RssItem;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.lifecycle.RequestContext;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Serves the RSS feed. The feed is rendered on demand from the in-memory
 * {@link BlogCache} through the {@code rss.xml} Qute template.
 */
@PermitAll
@Path("/rss")
public final class RssFeed {
    /**
     * Date time format specified by the RSS 2.0 specification (RFC 822).
     */
    private static final DateTimeFormatter RSS_SPEC_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z");

    /**
     * Default last build date for the RSS feed. It is set to the UNIX epoch and
     * used when the feed holds no blogs.
     */
    private static final String DEFAULT_LAST_BUILD_DATE = "Thu, 01 Jan 1970 00:00:00 UTC";

    /**
     * URL used by the website, should stay mijaljevic.xyz as long as I live I hope.
     */
    private static final String WEBSITE_URL = "https://mijaljevic.xyz/";

    /**
     * Value of the HTTP <i>Cache-Control</i> header applied to the feed.
     */
    private final String cacheControl;

    /**
     * Captures the request headers and provides the shared HTTP caching
     * utilities.
     */
    private final RequestContext requestContext;

    /**
     * The {@code rss.xml} Qute template that renders the feed.
     */
    private final Template rss;

    /**
     * The in-memory blog cache, the single source of truth for the feed items.
     */
    private final BlogCache blogCache;

    /**
     * Renders (and caches) the HTML body used as each item's description.
     */
    private final BlogRenderer blogRenderer;

    /**
     * Creates the resource with its configuration, request headers, template
     * and caches.
     *
     * @param cacheControl The HTTP <i>Cache-Control</i> header value.
     * @param requestContext The shared HTTP caching utilities.
     * @param rss          The {@code rss.xml} Qute template.
     * @param blogCache    The in-memory blog cache.
     * @param blogRenderer The on-demand blog HTML renderer.
     */
    @Inject
    public RssFeed(
            @ConfigProperty(name = "application.cache-control") final String cacheControl,
            final RequestContext requestContext,
            @Location("rss.xml") final Template rss,
            final BlogCache blogCache,
            final BlogRenderer blogRenderer
    ) {
        this.cacheControl = cacheControl;
        this.requestContext = requestContext;
        this.rss = rss;
        this.blogCache = blogCache;
        this.blogRenderer = blogRenderer;
    }

    /**
     * Serves the RSS feed XML with conditional-request caching headers. The feed
     * is rendered from the current {@link BlogCache} contents on every call.
     *
     * @return The RSS feed {@link Response}, or a 304 if the client cache is
     * current.
     */
    @GET
    @Produces(MediaType.TEXT_XML)
    public Response getRss() {
        final List<Blog> recent = blogCache.recent();

        final List<RssItem> items = new ArrayList<>();

        for (Blog blog : recent) {
            final String link = WEBSITE_URL + "blog/" + blog.getSlug();

            final String pubDate = blog.getCreated()
                    .atZone(Website.TIME_ZONE)
                    .format(RSS_SPEC_FORMAT);

            items.add(new RssItem(
                    blog.getTitle(),
                    link,
                    link,
                    blogRenderer.render(blog.getFileName()),
                    pubDate
            ));
        }

        final String lastBuildDate = recent.stream()
                .findFirst()
                .map(blog -> blog.getCreated()
                        .atZone(Website.TIME_ZONE)
                        .format(RSS_SPEC_FORMAT))
                .orElse(DEFAULT_LAST_BUILD_DATE);

        final String rssFeed = rss.data("lastBuildDate", lastBuildDate)
                .data("items", items)
                .render();

        final String etag = String.valueOf(rssFeed.hashCode());

        final String lastModified = RequestContext.parseLastModifiedTime(LocalDateTime.parse(
                lastBuildDate,
                RSS_SPEC_FORMAT
        ));

        final Response notModified = requestContext.notModified(etag, lastModified);

        if (notModified != null) return notModified;

        return Response.ok()
                .entity(rssFeed)
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }
}
