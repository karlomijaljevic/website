package xyz.mijaljevic.web;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
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
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.entity.Blog;
import xyz.mijaljevic.model.rss.Item;
import xyz.mijaljevic.model.rss.Rss;

/**
 * Serves the RSS feed.
 */
@PermitAll
@Path("/rss")
public final class RssFeed {
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

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

        String lastModified = WebHelper.parseLastModifiedTime(LocalDateTime.parse(lastBuildDate, RSS_SPEC_FORMAT));

        if (WebHelper.isResourceNotChanged(httpHeaders, etag, lastModified)) {
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
     */
    public static boolean initializeRssFeed(String rssFilePath) {
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
     */
    public static void updateRssFeed() {
        List<Item> items = new ArrayList<>();

        Website.retrieveRecentBlogs().forEach(blog -> {
            Item item = new Item();

            String filePath = Website.getBlogsDirectory().toString() + File.separator + blog.getFileName();

            try {
                item.setDescription(Files.readString(Paths.get(filePath), StandardCharsets.UTF_8));
            } catch (IOException e) {
                Log.error("Failed to read the '" + blog.getFileName() + "' blog contents for RSS!", e);

                return;
            }

            item.setGuid(WEBSITE_URL + "blog/" + blog.getId());
            item.setLink(WEBSITE_URL + "blog/" + blog.getId());
            item.setTitle(blog.getTitle());

            String pubDate = blog.getCreated().atZone(Website.TIME_ZONE).format(RSS_SPEC_FORMAT);
            item.setPubDate(pubDate);

            items.add(item);
        });

        Rss current = RSS_FEED.get();

        Blog lastUpdatedBlog = Website.BLOG_CACHE.values().stream().sorted().findFirst().orElse(null);

        if (lastUpdatedBlog != null) {
            String pubDate = lastUpdatedBlog.getCreated().atZone(Website.TIME_ZONE).format(RSS_SPEC_FORMAT);
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
            Log.fatal(e);

            ExitCodes.RSS_JAXB_CONTEXT_INIT_FAILED.logAndExit();
        }

        return context;
    }

    /**
     * Parses the provided file into a {@link Rss} instance. In case the provided
     * file was not an RSS XML the method returns null.
     *
     * @param file A {@link File} to parse
     * @return Returns a {@link Rss} instance. In case the provided file was not an
     * RSS XML the method returns null.
     */
    private static Rss readRssFeed(File file) {
        try {
            Unmarshaller jaxbUnmarshaller = RSS_JAXB_CONTEXT.createUnmarshaller();
            return (Rss) jaxbUnmarshaller.unmarshal(file);
        } catch (JAXBException e) {
            Log.error("Failed to parse the RSS feed XML file!", e);

            return null;
        }
    }

    /**
     * Transforms the static local {@link Rss} field <i>RSS_FEED</i> into an XML
     * {@link String}.
     *
     * @return A {@link String} holding the RSS feed or null in case an exception
     * occurs.
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
