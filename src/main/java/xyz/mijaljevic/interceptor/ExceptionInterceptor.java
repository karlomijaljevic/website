package xyz.mijaljevic.interceptor;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.net.URI;

/**
 * Intercepts exceptions that occur on the website and displays them on the
 * error page. This is a safety net, in case something goes wrong and an
 * exception is thrown that is not handled anywhere else.
 */
@Provider
public final class ExceptionInterceptor implements ExceptionMapper<Exception> {
    /**
     * Information about the request that triggered the exception, used to
     * enrich the log entry with the request method and path.
     */
    private final UriInfo uriInfo;

    /**
     * Creates the interceptor with the request {@link UriInfo}.
     *
     * @param uriInfo Information about the request that is being served.
     */
    public ExceptionInterceptor(@Context final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    @Override
    public Response toResponse(final Exception exception) {
        Log.errorf(
                exception,
                "Unhandled %s while serving '%s': %s",
                exception.getClass().getSimpleName(),
                uriInfo.getPath(),
                exception.getMessage()
        );

        return Response.seeOther(URI.create("/error/exception")).build();
    }
}
