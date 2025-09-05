package xyz.mijaljevic.web.interceptor;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
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
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.seeOther(URI.create("/error/not-found")).build();
    }
}
