package xyz.mijaljevic.web.interceptor;

import java.net.URI;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import xyz.mijaljevic.web.page.ErrorPage;

/**
 * Intercepts exceptions that occur on the website and displays the
 * {@link ErrorPage}. This class is a catch all for the exceptions that extend
 * the {@link Exception} class.
 */
@Provider
public final class ExceptionInterceptor implements ExceptionMapper<Exception>
{
	@Override
	public Response toResponse(Exception exception)
	{
		return Response.seeOther(URI.create("/error/exception")).build();
	}
}
