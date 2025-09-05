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
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.entity.StaticFile;

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
    @ConfigProperty(
            name = "application.css",
            defaultValue = "style.min.css"
    )
    String cssPath;

    @ConfigProperty(
            name = "application.javascript",
            defaultValue = "script.min.js"
    )
    String scriptPath;

    @ConfigProperty(
            name = "application.images-directory",
            defaultValue = "images"
    )
    String imagesDirectoryPath;

    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

    /**
     * {@link Pattern} of allowed image file names and extensions.
     */
    private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+\\.(jpg|png|jpeg|gif|ico)");

    /**
     * Maximum length of the image name, includes the file extension as well.
     */
    private static final int MAX_IMAGE_NAME_LENGTH = 80;

    /**
     * HTTP <i>Last-Modified</i> header for the css file.
     */
    private static final String LAST_MODIFIED = WebPage.parseLastModifiedTime(LocalDateTime.now());

    /**
     * HTTP <i>ETag</i> header for the css file.
     */
    private static final String E_TAG = WebPage.generateEtagHash(Instant.now().toString());

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
    public Response getImage(@PathParam(value = "name") String name) {
        if (name.isBlank() || name.contains(File.separator) || name.length() > MAX_IMAGE_NAME_LENGTH) {
            return returnBadRequest("The requested image name is NOT valid! Provided name: " + name);
        }

        Matcher match = IMAGE_NAME_PATTERN.matcher(name);

        if (!match.find()) {
            return returnBadRequest("The requested image name is NOT in proper format! Provided name: " + name);
        }

        StaticFile staticFile = Website.STATIC_CACHE
                .values()
                .stream()
                .filter(value -> value.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (staticFile == null) {
            return returnBadRequest("Client tried to find a image with an unknown name!");
        }

        String etag = staticFile.getHash();
        String lastModified = WebPage.parseLastModifiedTime(staticFile.getModified());

        String ifNoneMatch = httpHeaders.getHeaderString(HttpHeaders.IF_NONE_MATCH);
        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        String ifModifiedSince = httpHeaders.getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && ifModifiedSince.equals(lastModified)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        String pathString = imagesDirectoryPath + File.separator + name;

        java.nio.file.Path path = Paths.get(pathString);

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
    private static Response returnBadRequest(String message) {
        JsonObject response = new JsonObject();

        response.put("message", message);

        return Response.status(Status.BAD_REQUEST).entity(response).build();
    }
}
