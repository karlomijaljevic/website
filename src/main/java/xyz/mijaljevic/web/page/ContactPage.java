package xyz.mijaljevic.web.page;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.web.WebHelper;
import xyz.mijaljevic.web.WebKeys;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Displays my contacts to the user.
 */
@PermitAll
@Path("/contact")
public final class ContactPage {
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    Template contactPage;

    /**
     * HTML title of the contact page.
     */
    private static final String TITLE = "Contact";

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

        TemplateInstance template = contactPage.data(WebKeys.TITLE, TITLE);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, E_TAG)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, LAST_MODIFIED)
                .build();
    }
}
