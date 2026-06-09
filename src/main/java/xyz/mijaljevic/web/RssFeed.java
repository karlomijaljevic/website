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

package xyz.mijaljevic.web;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.domain.rss.Item;
import xyz.mijaljevic.domain.rss.Rss;
import xyz.mijaljevic.utils.MarkdownParser;

import java.io.File;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Serves the RSS feed.
 */
@PermitAll
@Path("/rss")
public final class RssFeed {
    /**
     * Value of the HTTP <i>Cache-Control</i> header applied to the feed.
     */
    private final String cacheControl;

    /**
     * Incoming request headers, used for conditional-request comparisons.
     */
    private final HttpHeaders httpHeaders;

    /**
     * Creates the resource with its configuration and request headers.
     *
     * @param cacheControl The HTTP <i>Cache-Control</i> header value.
     * @param httpHeaders  The incoming request {@link HttpHeaders}.
     */
    @Inject
    public RssFeed(
            @ConfigProperty(name = "application.cache-control") final String cacheControl,
            final HttpHeaders httpHeaders
    ) {
        this.cacheControl = cacheControl;
        this.httpHeaders = httpHeaders;
    }

    /**
     * Date time format specified by the RSS 2.0 specification (RFC 822).
     */
    private static final DateTimeFormatter RSS_SPEC_FORMAT = DateTimeFormatter.ofPattern("E, dd MMM yyyy HH:mm:ss z");

    /**
     * Default last build date for the RSS feed. It is set to the UNIX epoch.
     */
    private static final String DEFAULT_LAST_BUILD_DATE = "Thu, 01 Jan 1970 00:00:00 UTC";

    /**
     * URL used by the website, should stay mijaljevic.xyz as long as I live I hope.
     */
    private static final String WEBSITE_URL = "https://mijaljevic.xyz/";

    /**
     * {@link JAXBContext} tied to the {@link Rss} class.
     */
    private static final JAXBContext RSS_JAXB_CONTEXT = getRssContext();

    /**
     * RSS feed XML file served by the GET request to this endpoint.
     */
    private static final AtomicReference<Rss> RSS_FEED = new AtomicReference<>(null);

    /**
     * Serves the RSS feed XML with conditional-request caching headers.
     *
     * @return The RSS feed {@link Response}, or a 304 if the client cache is
     * current.
     */
    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_XML)
    public Response getRss() {
        String rssFeed = fetchRssFeed();

        String etag = String.valueOf(rssFeed != null ? rssFeed.hashCode() : "");

        String lastBuildDate = RSS_FEED.get().getChannel().getLastBuildDate();

        if (lastBuildDate == null || lastBuildDate.isBlank()) {
            lastBuildDate = DEFAULT_LAST_BUILD_DATE;
        }

        String lastModified = WebPage.parseLastModifiedTime(LocalDateTime.parse(
                lastBuildDate,
                RSS_SPEC_FORMAT
        ));

        String ifNoneMatch = httpHeaders.getHeaderString(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        String ifModifiedSince = httpHeaders.getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && ifModifiedSince.equals(lastModified)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        return Response.ok()
                .entity(rssFeed)
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Tries to initialize the RSS feed.
     *
     * @param rssFilePath The path to the RSS template file.
     * @return Returns true on success and false on failure.
     * @throws NullPointerException if {@code rssFilePath} is null.
     */
    public static boolean initializeRssFeed(final String rssFilePath) {
        Objects.requireNonNull(rssFilePath, "rssFilePath must not be null");

        Rss rss = readRssFeed(new File(rssFilePath));

        if (rss == null) {
            return false;
        }

        rss.getChannel().setLastBuildDate(DEFAULT_LAST_BUILD_DATE);

        RSS_FEED.set(rss);

        return true;
    }

    /**
     * Updates the items served by the RSS feed.
     *
     * @param blogsDirectoryPath The path to the blogs' directory.
     * @throws NullPointerException if {@code blogsDirectoryPath} is null.
     */
    public static void updateRssFeed(final String blogsDirectoryPath) {
        Objects.requireNonNull(blogsDirectoryPath, "blogsDirectoryPath must not be null");

        List<Item> items = new ArrayList<>();

        Website.retrieveRecentBlogs().forEach(blog -> {
            Item item = new Item();

            String filePath = blogsDirectoryPath
                    + File.separator
                    + blog.getFileName();

            item.setDescription(MarkdownParser.renderMarkdownToHtml(new File(filePath)));
            item.setGuid(WEBSITE_URL + "blog/" + blog.getId());
            item.setLink(WEBSITE_URL + "blog/" + blog.getId());
            item.setTitle(blog.getTitle());

            String pubDate = blog.getCreated()
                    .atZone(Website.TIME_ZONE)
                    .format(RSS_SPEC_FORMAT);

            item.setPubDate(pubDate);

            items.add(item);
        });

        Rss current = RSS_FEED.get();

        Blog lastUpdatedBlog = Website.BLOG_CACHE
                .values()
                .stream()
                .sorted()
                .findFirst()
                .orElse(null);

        if (lastUpdatedBlog != null) {
            String pubDate = lastUpdatedBlog.getCreated()
                    .atZone(Website.TIME_ZONE)
                    .format(RSS_SPEC_FORMAT);
            current.getChannel().setLastBuildDate(pubDate);
        }

        current.getChannel().setItems(items);

        RSS_FEED.set(current);
    }

    /**
     * @return Returns the {@link JAXBContext} tied to the {@link Rss} class.
     */
    private static JAXBContext getRssContext() {
        JAXBContext context = null;

        try {
            context = JAXBContext.newInstance(Rss.class);
        } catch (JAXBException e) {
            Log.fatal("Failed to create JAXB context for the Rss class!", e);
            Quarkus.asyncExit();
        }

        return context;
    }

    /**
     * Parses the provided file into a {@link Rss} instance. In case the
     * provided file was not an RSS XML the method returns null.
     *
     * @param file A {@link File} to parse
     * @return Returns a {@link Rss} instance. In case the provided file was
     * not an RSS XML the method returns null.
     */
    private static Rss readRssFeed(final File file) {
        try {
            Unmarshaller jaxbUnmarshaller = RSS_JAXB_CONTEXT.createUnmarshaller();
            return (Rss) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            Log.errorf(e, "Failed to parse the RSS feed XML file: %s", file);
            return null;
        }
    }

    /**
     * Transforms the static local {@link Rss} field <i>RSS_FEED</i> into an
     * XML {@link String}.
     *
     * @return A {@link String} holding the RSS feed or null in case an
     * exception occurs.
     */
    private static String fetchRssFeed() {
        try {
            StringWriter stringWriter = new StringWriter();
            Marshaller marshaller = RSS_JAXB_CONTEXT.createMarshaller();

            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(RSS_FEED.get(), stringWriter);

            return stringWriter.toString();
        } catch (JAXBException e) {
            Log.error("Failed to marshal the RSS instance into a String instance!", e);
            return null;
        }
    }
}
