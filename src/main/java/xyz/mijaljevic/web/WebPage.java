package xyz.mijaljevic.web;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.cache.BlogCache;
import xyz.mijaljevic.cache.BlogRenderer;
import xyz.mijaljevic.domain.dto.BlogLink;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.lifecycle.RequestContext;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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
     * The in-memory cache that is the single source of truth for blogs.
     */
    private final BlogCache blogCache;

    /**
     * Renders (and caches) the HTML body of a blog on demand.
     */
    private final BlogRenderer blogRenderer;

    /**
     * Captures the request headers and provides the shared HTTP caching
     * utilities.
     */
    private final RequestContext requestContext;

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
     * @param blogCache          The in-memory blog cache.
     * @param blogRenderer       The on-demand blog HTML renderer.
     * @param requestContext The shared HTTP caching utilities.
     * @param homePage           The home page template.
     * @param blogPage           The single blog page template.
     * @param allBlogsPage       The all blogs listing template.
     * @param contactPage        The contact page template.
     * @param errorPage          The error page template.
     */
    @Inject
    public WebPage(
            @ConfigProperty(name = "application.cache-control") final String cacheControl,
            final BlogCache blogCache,
            final BlogRenderer blogRenderer,
            final RequestContext requestContext,
            final Template homePage,
            final Template blogPage,
            final Template allBlogsPage,
            final Template contactPage,
            final Template errorPage
    ) {
        this.cacheControl = cacheControl;
        this.blogCache = blogCache;
        this.blogRenderer = blogRenderer;
        this.requestContext = requestContext;
        this.homePage = homePage;
        this.blogPage = blogPage;
        this.allBlogsPage = allBlogsPage;
        this.contactPage = contactPage;
        this.errorPage = errorPage;
    }

    /**
     * HTTP <i>Last-Modified</i> header. Updated periodically when a blog gets
     * updated or created. Used by pages which need be updated when a new blog
     * is added, for example the home page.
     */
    private static final AtomicReference<String> LAST_MODIFIED = new AtomicReference<>(
            RequestContext.parseLastModifiedTime(LocalDateTime.now())
    );

    /**
     * HTTP <i>ETag</i> header. Updated periodically when a blog gets updated
     * or created. Used by pages which need be updated when a new blog is
     * added, for example the home page.
     */
    private static final AtomicReference<String> E_TAG = new AtomicReference<>(
            RequestContext.generateEtagHash(Instant.now().toString())
    );

    /**
     * Serves the home page, honoring conditional-request caching headers.
     *
     * @return The rendered home page, or a 304 if the client cache is current.
     */
    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getHomePage() {
        final String eTag = E_TAG.getOpaque();
        final String lastModified = LAST_MODIFIED.getOpaque();

        final Response notModified = requestContext.notModified(eTag, lastModified);

        if (notModified != null) return notModified;

        final List<BlogLink> blogs = new ArrayList<>();

        blogCache.recent()
                .forEach(blog -> blogs.add(BlogLink.generateBlogLinkFromBlog(blog)));

        final TemplateInstance template = homePage.data("title", "Karlo Mijaljevic")
                .data("blogs", blogs);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Serves a single blog page for the requested blog slug.
     *
     * @param slug The slug of the blog to render.
     * @return The rendered blog page, or a 304 if the client cache is current.
     */
    @GET
    @Path("/blog/{slug}")
    @Produces(MediaType.TEXT_HTML)
    public Response getBlogPage(@PathParam("slug") final String slug) {
        final Blog blog = blogCache.bySlug(slug);

        if (blog == null) {
            throw new NotFoundException("Client tried to find a blog with an unknown slug!");
        }

        final LocalDateTime lastUpdated = blog.getUpdated() == null
                ? blog.getCreated()
                : blog.getUpdated();

        final String etag = blog.getHash();
        final String lastModified = RequestContext.parseLastModifiedTime(lastUpdated);

        final Response notModified = requestContext.notModified(etag, lastModified);

        if (notModified != null) return notModified;

        final String data = blogRenderer.render(blog.getFileName());

        final TemplateInstance template = blogPage.data("blog", blog)
                .data("data", data)
                .data("title", blog.getTitle());

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Serves the page listing all blogs, sorted, honoring caching headers.
     *
     * @return The rendered blog list, or a 304 if the client cache is current.
     */
    @GET
    @NonBlocking
    @Path("/blogs")
    @Produces(MediaType.TEXT_HTML)
    public Response getBlogsPage() {
        final String eTag = E_TAG.getOpaque();
        final String lastModified = LAST_MODIFIED.getOpaque();

        final Response notModified = requestContext.notModified(eTag, lastModified);

        if (notModified != null) return notModified;

        final List<BlogLink> blogs = blogCache.all()
                .stream()
                .map(BlogLink::generateBlogLinkFromBlog)
                .toList();

        final TemplateInstance template = allBlogsPage.data("blogs", blogs)
                .data("title", "My Blogs");

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Serves the contact page, honoring conditional-request caching headers.
     *
     * @return The rendered contact page, or a 304 if the client cache is
     * current.
     */
    @GET
    @NonBlocking
    @Path("/contact")
    @Produces(MediaType.TEXT_HTML)
    public Response getContactsPage() {
        final String eTag = E_TAG.getOpaque();
        final String lastModified = LAST_MODIFIED.getOpaque();

        final Response notModified = requestContext.notModified(eTag, lastModified);

        if (notModified != null) return notModified;

        final TemplateInstance template = contactPage.data("title", "Contact");

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
        final String eTag = E_TAG.getOpaque();
        final String lastModified = LAST_MODIFIED.getOpaque();

        final Response notModified = requestContext.notModified(eTag, lastModified);

        if (notModified != null) return notModified;

        final String status = switch (reason) {
            case "not-found" -> "404 Not Found";
            case "bad-request" -> "400 Bad Request";
            case "exception" -> "500 Internal Server Error";
            case null, default -> "418 I'm a teapot";
        };

        final TemplateInstance template = errorPage.data("status", status)
                .data("title", status);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Updates the <i>Etag</i> and <i>Last-Modified</i> headers used by pages
     * which monitor new blogs.
     */
    public static void updateCacheControlHeaders() {
        E_TAG.set(RequestContext.generateEtagHash(Instant.now().toString()));
        LAST_MODIFIED.set(RequestContext.parseLastModifiedTime(LocalDateTime.now()));
    }
}
