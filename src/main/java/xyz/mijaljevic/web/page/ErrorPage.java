package xyz.mijaljevic.web.page;

import java.time.Instant;
import java.time.LocalDateTime;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import xyz.mijaljevic.web.WebHelper;
import xyz.mijaljevic.web.WebKeys;

/**
 * Displays an error message to the user. Depending on the reason.
 */
@PermitAll
@Path("/error/{reason}")
public final class ErrorPage {
    @PathParam(value = "reason")
    private String reason;

    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    Template errorPage;

    /**
     * HTTP <i>ETag</i> header.
     */
    private static final String E_TAG = WebHelper.generateEtagHash(Instant.now().toString());

    /**
     * HTTP <i>Last-Modified</i> header.
     */
    private static final String LAST_MODIFIED = WebHelper.parseLastModifiedTime(LocalDateTime.now());

    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getPage() {
        if (WebHelper.isResourceNotChanged(httpHeaders, E_TAG, LAST_MODIFIED)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        String status;

        if ("not-found".equals(reason)) {
            status = "404 Not Found";
        } else if ("exception".equals(reason)) {
            status = "500 Internal Server Error";
        } else {
            status = "418 I'm a teapot";
        }

        TemplateInstance template = errorPage.data(WebKeys.STATUS, status).data(WebKeys.TITLE, status);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, E_TAG)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED)
                .build();
    }
}
