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
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.runtime.Quarkus;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.dto.BlogLink;
import xyz.mijaljevic.domain.entity.Blog;
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
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * JAX-RS resource that serves the public HTML pages of the website (home, blog,
 * blog list, contact and error pages) rendered through Qute templates. Handles
 * HTTP caching via <i>ETag</i> and <i>Last-Modified</i> headers.
 */
@PermitAll
@Path("/")
public final class WebPage {
    /**
     * Value of the HTTP <i>Cache-Control</i> header applied to served pages.
     */
    private final String cacheControl;

    /**
     * The path to the blogs' directory.
     */
    private final String blogsDirectoryPath;

    /**
     * Incoming request headers, used for conditional-request comparisons.
     */
    private final HttpHeaders httpHeaders;

    /**
     * Qute template for the home page.
     */
    private final Template homePage;

    /**
     * Qute template for a single blog page.
     */
    private final Template blogPage;

    /**
     * Qute template for the page listing all blogs.
     */
    private final Template allBlogsPage;

    /**
     * Qute template for the contact page.
     */
    private final Template contactPage;

    /**
     * Qute template for the error page.
     */
    private final Template errorPage;

    /**
     * Creates the resource with its configuration and injected Qute templates.
     *
     * @param cacheControl       The HTTP <i>Cache-Control</i> header value.
     * @param blogsDirectoryPath The path to the blogs' directory.
     * @param httpHeaders        The incoming request {@link HttpHeaders}.
     * @param homePage           The home page template.
     * @param blogPage           The single blog page template.
     * @param allBlogsPage       The all blogs listing template.
     * @param contactPage        The contact page template.
     * @param errorPage          The error page template.
     */
    @Inject
    public WebPage(
            @ConfigProperty(name = "application.cache-control") final String cacheControl,
            @ConfigProperty(
                    name = "application.blogs-directory",
                    defaultValue = "blogs"
            ) final String blogsDirectoryPath,
            final HttpHeaders httpHeaders,
            final Template homePage,
            final Template blogPage,
            final Template allBlogsPage,
            final Template contactPage,
            final Template errorPage
    ) {
        this.cacheControl = cacheControl;
        this.blogsDirectoryPath = blogsDirectoryPath;
        this.httpHeaders = httpHeaders;
        this.homePage = homePage;
        this.blogPage = blogPage;
        this.allBlogsPage = allBlogsPage;
        this.contactPage = contactPage;
        this.errorPage = errorPage;
    }

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

    /**
     * Serves the home page, honouring conditional-request caching headers.
     *
     * @return The rendered home page, or a 304 if the client cache is current.
     */
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

    /**
     * Serves a single blog page for the requested blog ID.
     *
     * @param id The ID of the blog to render.
     * @return The rendered blog page, or a 304 if the client cache is current.
     */
    @GET
    @NonBlocking
    @Path("/blog/{id}")
    @Produces(MediaType.TEXT_HTML)
    public Response getBlogPage(@PathParam("id") final Long id) {
        if (id == null) {
            throw new BadRequestException("Client tried to find a blog with a null ID!");
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
                Log.errorf(e, "Failed to display the blog with id %s", id);

                throw new IllegalStateException("Page rendering for blog with ID " + id + " failed!", e);
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

    /**
     * Serves the page listing all blogs, sorted, honouring caching headers.
     *
     * @return The rendered blog list, or a 304 if the client cache is current.
     */
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

    /**
     * Serves the contact page, honouring conditional-request caching headers.
     *
     * @return The rendered contact page, or a 304 if the client cache is
     * current.
     */
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

    /**
     * Serves the error page for the supplied reason code.
     *
     * @param reason The error reason path segment (e.g. {@code not-found}).
     * @return The rendered error page, or a 304 if the client cache is current.
     */
    @GET
    @NonBlocking
    @Path("/error/{reason}")
    @Produces(MediaType.TEXT_HTML)
    public Response getErrorPage(@PathParam("reason") final String reason) {
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
            final LocalDateTime lastModifiedTime
    ) {
        return lastModifiedTime.atZone(Website.TIME_ZONE).format(LM_FORMAT);
    }

    /**
     * Generates an <i>ETag</i> hash from the provided string.
     *
     * @param string A {@link String} to turn into an ETag hash.
     * @return Returns a hexadecimal hash from the provided string.
     * @throws NullPointerException if {@code string} is null.
     */
    static String generateEtagHash(final String string) {
        Objects.requireNonNull(string, "string for E_TAG hash must not be null");

        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(Website.HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Log.fatalf(e, "Missing hashing algorithm: %s", Website.HASH_ALGORITHM);
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
            final Blog blog,
            final String blogsDirectoryPath
    ) throws IOException {
        // NOTE: Check if it has been synced by a closely timed thread
        final Blog synced = Website.BLOG_CACHE.get(blog.getFileName());

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
    private boolean isResourceSame(final String etag, final String lastModified) {
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
