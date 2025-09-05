package xyz.mijaljevic.web.interceptor;

import jakarta.ws.rs.core.Response;
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
    @Override
    public Response toResponse(Exception exception) {
        return Response.seeOther(URI.create("/error/exception")).build();
    }
}
