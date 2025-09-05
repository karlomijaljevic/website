package xyz.mijaljevic.web;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.Quarkus;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.dto.BlogLink;
import xyz.mijaljevic.model.entity.Blog;
import xyz.mijaljevic.utils.MarkdownParser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@PermitAll
@Path("/")
public final class WebPage {
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    /**
     * The path to the blogs' directory.
     */
    @ConfigProperty(
            name = "application.blogs-directory",
            defaultValue = "blogs"
    )
    String blogsDirectoryPath;

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    Template homePage;

    @Inject
    Template blogPage;

    @Inject
    Template allBlogsPage;

    @Inject
    Template contactPage;

    @Inject
    Template errorPage;

    /**
     * HTTP <i>Last-Modified</i> date format.
     */
    private static final DateTimeFormatter LM_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

    /**
     * HTTP <i>Last-Modified</i> header. Updated periodically when a blog gets
     * updated or created. Used by pages which need be updated when a new blog
     * is added, for example the home page.
     */
    private static final AtomicReference<String> LAST_MODIFIED = new AtomicReference<>(
            parseLastModifiedTime(LocalDateTime.now())
    );

    /**
     * HTTP <i>ETag</i> header. Updated periodically when a blog gets updated
     * or created. Used by pages which need be updated when a new blog is
     * added, for example the home page.
     */
    private static final AtomicReference<String> E_TAG = new AtomicReference<>(
            generateEtagHash(Instant.now().toString())
    );

    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getHomePage() {
        String eTag = E_TAG.getOpaque();
        String lastModified = LAST_MODIFIED.getOpaque();

        if (isResourceSame(eTag, lastModified)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        List<BlogLink> blogs = new ArrayList<>();

        Website.retrieveRecentBlogs()
                .forEach(blog -> blogs.add(BlogLink.generateBlogLinkFromBlog(blog)));

        TemplateInstance template = homePage.data("title", "Karlo Mijaljevic")
                .data("blogs", blogs);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    @GET
    @NonBlocking
    @Path("/blog/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Response getBlogPage(@PathParam("id") Long id) {
        if (id == null) {
            throw new BadRequestException("Client tried to find a blog with a null or blank ID!");
        }

        Blog blog = Website.BLOG_CACHE
                .values()
                .stream()
                .filter(value -> value.getId().equals(id))
                .findFirst()
                .orElse(null);

        if (blog == null) {
            throw new NotFoundException("Client tried to find a blog with an unknown ID!");
        }

        LocalDateTime lastUpdated = blog.getUpdated() == null
                ? blog.getCreated()
                : blog.getUpdated();

        String etag = blog.getHash();
        String lastModified = parseLastModifiedTime(lastUpdated);

        if (isResourceSame(etag, lastModified)) {
            Website.BLOG_CACHE.get(blog.getFileName())
                    .setLastRead(LocalDateTime.now());

            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        if (blog.getData() == null) {
            try {
                blog = syncBlogData(blog, blogsDirectoryPath);
            } catch (IOException e) {
                Log.error("Failed to display the blog with id " + id, e);

                throw new IllegalStateException("Page rendering for blog with ID " + id + " failed!");
            }
        }

        Website.BLOG_CACHE.get(blog.getFileName())
                .setLastRead(LocalDateTime.now());

        TemplateInstance template = blogPage.data("blog", blog)
                .data("title", blog.getTitle());

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    @GET
    @NonBlocking
    @Path("/blogs")
    @Produces(MediaType.TEXT_HTML)
    public Response getBlogsPage() {
        String eTag = E_TAG.getOpaque();
        String lastModified = LAST_MODIFIED.getOpaque();

        if (isResourceSame(eTag, lastModified)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        List<BlogLink> blogs = Website.BLOG_CACHE.values()
                .stream()
                .sorted()
                .map(BlogLink::generateBlogLinkFromBlog)
                .toList();

        TemplateInstance template = allBlogsPage.data("blogs", blogs)
                .data("title", "My Blogs");

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    @GET
    @NonBlocking
    @Path("/contact")
    @Produces(MediaType.TEXT_HTML)
    public Response getContactsPage() {
        String eTag = E_TAG.getOpaque();
        String lastModified = LAST_MODIFIED.getOpaque();

        if (isResourceSame(eTag, lastModified)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        TemplateInstance template = contactPage.data("title", "Contact");

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    @GET
    @NonBlocking
    @Path("/error/{reason}")
    @Produces(MediaType.TEXT_HTML)
    public Response getErrorPage(@PathParam("reason") String reason) {
        String eTag = E_TAG.getOpaque();
        String lastModified = LAST_MODIFIED.getOpaque();

        if (isResourceSame(eTag, lastModified)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        String status = switch (reason) {
            case "not-found" -> "404 Not Found";
            case "bad-request" -> "400 Bad Request";
            case "exception" -> "500 Internal Server Error";
            case null, default -> "418 I'm a teapot";
        };

        TemplateInstance template = errorPage.data("status", status)
                .data("title", status);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, E_TAG)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED)
                .build();
    }

    /**
     * Updates the <i>Etag</i> and <i>Last-Modified</i> headers used by pages
     * which monitor new blogs.
     */
    public static void updateCacheControlHeaders() {
        E_TAG.set(generateEtagHash(Instant.now().toString()));
        LAST_MODIFIED.set(parseLastModifiedTime(LocalDateTime.now()));
    }

    /**
     * Converts the provided {@link LocalDateTime} into an HTTP
     * <i>Last-Modified</i> header value.
     *
     * @param lastModifiedTime A {@link LocalDateTime} instance.
     * @return An HTTP <i>Last-Modified</i> header {@link String}.
     */
    static String parseLastModifiedTime(
            LocalDateTime lastModifiedTime
    ) {
        return lastModifiedTime.atZone(Website.TIME_ZONE).format(LM_FORMAT);
    }

    /**
     * Generates an <i>ETag</i> hash from the provided string.
     *
     * @param string A {@link String} to turn into an ETag hash.
     * @return Returns a hexadecimal hash from the provided string.
     */
    static String generateEtagHash(String string) {
        if (string == null) {
            throw new NullPointerException("Provided string for E_TAG hash is null!");
        }

        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(Website.HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Log.fatal("Missing hashing algorithm: " + Website.HASH_ALGORITHM, e);
            Quarkus.asyncExit();
        }

        if (digest == null) {
            throw new NullPointerException("Digest for E_TAG hash is null!");
        }

        byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));

        StringBuilder hexString = new StringBuilder(2 * hash.length);

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);

            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Reads the data from the blog file and puts the updated blog reference
     * back into the blogs cache updating the data.
     *
     * @param blog               The {@link Blog} entity that has been
     *                           requested.
     * @param blogsDirectoryPath The path to the blogs' directory.
     * @return Returns the updated {@link Blog} reference.
     * @throws IOException In case it failed to read the blog data from the
     *                     file.
     */
    private static synchronized Blog syncBlogData(
            Blog blog,
            String blogsDirectoryPath
    ) throws IOException {
        // Check if it has been synced by a close called thread
        Blog synced = Website.BLOG_CACHE.get(blog.getFileName());

        if (synced.getData() != null) {
            return synced;
        }

        String filePath = blogsDirectoryPath + File.separator + blog.getFileName();

        blog.setData(MarkdownParser.renderMarkdownToHtml(new File(filePath)));

        Website.BLOG_CACHE.put(blog.getFileName(), blog);

        return blog;
    }

    /**
     * Compares the <i>Etag</i> header with the <i>If-None-Match</i> header and
     * the <i>Last-Modified</i> header with the <i>If-Modified-Since</i> header.
     *
     * <p>
     * If there have been changes between the provided headers and the ones in
     * the injected {@link HttpHeaders} instance the method returns true. The
     * priority is given to the <i>Etag</i> and <i>If-None-Match</i> comparison.
     * </p>
     *
     * @param etag         The HTTP ETag header that the page is currently
     *                     serving.
     * @param lastModified The HTTP Last-Modified that the page is currently
     *                     serving.
     * @return If there have been changes between the provided headers and the
     * ones in the injected {@link HttpHeaders} instance the method returns
     * true.
     */
    private boolean isResourceSame(String etag, String lastModified) {
        String ifNoneMatch = httpHeaders.getHeaderString(HttpHeaders.IF_NONE_MATCH);
        String ifModifiedSince = httpHeaders.getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);

        if (ifNoneMatch != null) {
            return ifNoneMatch.equals(etag);
        } else if (ifModifiedSince != null) {
            return ifModifiedSince.equals(lastModified);
        } else {
            return false;
        }
    }
}
