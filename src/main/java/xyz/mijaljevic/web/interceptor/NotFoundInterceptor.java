package xyz.mijaljevic.web.interceptor;

import java.net.URI;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import xyz.mijaljevic.web.page.ErrorPage;

/**
 * Intercepts <i><b>404 NOT FOUND</b></i> exceptions that occur on the website
 * and displays the {@link ErrorPage}. This class is a catch-all for the
 * exceptions that extend the {@link NotFoundException} class.
 */
@Provider
public final class NotFoundInterceptor implements ExceptionMapper<NotFoundException> {
    @Override
    public Response toResponse(NotFoundException exception) {
        return Response.seeOther(URI.create("/error/not-found")).build();
    }
}
