package xyz.mijaljevic.interceptor;

import io.quarkus.logging.Log;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

/**
 * Intercepts <i><b>404 NOT FOUND</b></i> exceptions that occur on the website
 * and displays them on the error page. This is done to avoid displaying the
 * default Quarkus error page.
 */
@Provider
public final class NotFoundInterceptor implements ExceptionMapper<NotFoundException> {
    /**
     * Information about the request that triggered the exception, used to
     * enrich the log entry with the requested path.
     */
    private final UriInfo uriInfo;

    /**
     * Creates the interceptor with the request {@link UriInfo}.
     *
     * @param uriInfo Information about the request that is being served.
     */
    public NotFoundInterceptor(@Context final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Response toResponse(final NotFoundException exception) {
        Log.debugf("Resource not found: '%s'", uriInfo.getPath());

        return Response.seeOther(URI.create("/error/not-found")).build();
    }
}
