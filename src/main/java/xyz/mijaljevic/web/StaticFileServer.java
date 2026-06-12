package xyz.mijaljevic.web;

import io.smallrye.common.annotation.NonBlocking;
import io.vertx.core.json.JsonObject;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.cache.StaticFileCache;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.lifecycle.RequestContext;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The static file server. Serves CSS files, image files and any other future
 * static files.
 */
@PermitAll
@Path("/static")
public final class StaticFileServer {
    /**
     * Path to the CSS file served by this resource.
     */
    private final String cssPath;

    /**
     * Path to the JavaScript file served by this resource.
     */
    private final String scriptPath;

    /**
     * Path to the directory holding served images.
     */
    private final String imagesDirectoryPath;

    /**
     * Value of the HTTP <i>Cache-Control</i> header applied to served files.
     */
    private final String cacheControl;

    /**
     * The in-memory cache that is the single source of truth for static files.
     */
    private final StaticFileCache staticFileCache;

    /**
     * Captures the request headers and provides the shared HTTP caching
     * utilities.
     */
    private final RequestContext requestContext;

    /**
     * Creates the resource with its configuration and request headers.
     *
     * @param cssPath             The path to the CSS file.
     * @param scriptPath          The path to the JavaScript file.
     * @param imagesDirectoryPath The path to the images' directory.
     * @param cacheControl        The HTTP <i>Cache-Control</i> header value.
     * @param staticFileCache     The in-memory static file cache.
     * @param requestContext  The shared HTTP caching utilities.
     */
    @Inject
    public StaticFileServer(
            @ConfigProperty(
                    name = "application.css",
                    defaultValue = "style.min.css"
            ) final String cssPath,
            @ConfigProperty(
                    name = "application.javascript",
                    defaultValue = "script.min.js"
            ) final String scriptPath,
            @ConfigProperty(
                    name = "application.images-directory",
                    defaultValue = "images"
            ) final String imagesDirectoryPath,
            @ConfigProperty(name = "application.cache-control") final String cacheControl,
            final StaticFileCache staticFileCache,
            final RequestContext requestContext
    ) {
        this.cssPath = cssPath;
        this.scriptPath = scriptPath;
        this.imagesDirectoryPath = imagesDirectoryPath;
        this.cacheControl = cacheControl;
        this.staticFileCache = staticFileCache;
        this.requestContext = requestContext;
    }

    /**
     * {@link Pattern} of allowed image file names and extensions.
     */
    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+\\.(jpg|png|jpeg|gif|ico)");

    /**
     * Maximum length of the image name, includes the file extension as well.
     */
    private static final int MAX_IMAGE_NAME_LENGTH = 80;

    /**
     * HTTP <i>Last-Modified</i> header for the CSS file.
     */
    private static final String LAST_MODIFIED = RequestContext.parseLastModifiedTime(LocalDateTime.now());

    /**
     * HTTP <i>ETag</i> header for the CSS file.
     */
    private static final String E_TAG = RequestContext.generateEtagHash(Instant.now().toString());

    /**
     * Serves the CSS file with caching headers.
     *
     * @return The CSS file {@link Response}.
     */
    @GET
    @NonBlocking
    @Path("/style.min.css")
    @Produces(value = "text/css")
    public Response getCss() {
        return Response.ok()
                .entity(Paths.get(cssPath))
                .header(HttpHeaders.ETAG, E_TAG)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED)
                .build();
    }

    /**
     * Serves the JavaScript file with caching headers.
     *
     * @return The JavaScript file {@link Response}.
     */
    @GET
    @NonBlocking
    @Path("/script.min.js")
    @Produces(value = "application/javascript")
    public Response getJs() {
        return Response.ok()
                .entity(Paths.get(scriptPath))
                .header(HttpHeaders.ETAG, E_TAG)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED)
                .build();
    }

    /**
     * Serves the requested image by name with caching headers, validating the
     * name and falling back to a bad-request or not-found response.
     *
     * @param name The requested image file name.
     * @return The image {@link Response}, or an error response if invalid or
     * missing.
     */
    @GET
    @NonBlocking
    @Path("/image/{name}")
    @Produces(value = {
            "image/png",
            "image/jpeg",
            "image/gif",
            "image/x-icon",
            "application/json"
    })
    public Response getImage(@PathParam(value = "name") final String name) {
        if (name.isBlank() || name.contains(File.separator) || name.length() > MAX_IMAGE_NAME_LENGTH) {
            return returnBadRequest("The requested image name is NOT valid! Provided name: " + name);
        }

        final Matcher match = IMAGE_NAME_PATTERN.matcher(name);

        if (!match.matches()) {
            return returnBadRequest("The requested image name is NOT in proper format! Provided name: " + name);
        }

        final StaticFile staticFile = staticFileCache.byName(name);

        if (staticFile == null) {
            return returnBadRequest("Client tried to find a image with an unknown name!");
        }

        final String etag = staticFile.getHash();
        final String lastModified = RequestContext.parseLastModifiedTime(staticFile.getModified());

        final Response notModified = requestContext.notModified(etag, lastModified);

        if (notModified != null) return notModified;

        final String pathString = imagesDirectoryPath + File.separator + name;

        final java.nio.file.Path path = Paths.get(pathString);

        if (!Files.exists(path, LinkOption.NOFOLLOW_LINKS)) {
            return Response.status(Status.NOT_FOUND).build();
        }

        return Response.ok()
                .entity(path)
                .header(HttpHeaders.ETAG, etag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }

    /**
     * Creates a <b>BAD_REQUEST</b> {@link Response} instance with a
     * {@link JsonObject} entity.
     *
     * @param message A message to attach to the JSON object.
     * @return A <b>BAD_REQUEST</b> {@link Response} instance with a
     * {@link JsonObject} entity.
     */
    private static Response returnBadRequest(final String message) {
        final JsonObject response = new JsonObject();

        response.put("message", message);

        return Response.status(Status.BAD_REQUEST).entity(response).build();
    }
}
