package xyz.mijaljevic.web;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.entity.StaticFile;

/**
 * The static file server. Serves CSS files, image files and any other future
 * static files.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0
 */
@PermitAll
@Path("/static")
public final class StaticFileServer
{
	@ConfigProperty(name = "application.css-directory", defaultValue = "css")
	private String cssDirectoryPath;

	@ConfigProperty(name = "application.images-directory", defaultValue = "images")
	private String imagesDirectoryPath;

	@ConfigProperty(name = "application.cache-control")
	private String cacheControl;

	@Inject
	private HttpHeaders httpHeaders;

	/**
	 * {@link Pattern} of allowed image file names and extensions.
	 */
	private static final Pattern IMAGE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+\\.(jpg|png|jpeg|gif|ico)");

	/**
	 * {@link Pattern} of allowed CSS file names.
	 */
	private static final Pattern CSS_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_\\-]+\\.css");

	/**
	 * Maximum length of the image name, includes the file extension as well.
	 */
	private static final int MAX_IMAGE_NAME_LENGTH = 80;

	/**
	 * Maximum length of the CSS file name, includes the file extension as well.
	 */
	private static final int MAX_CSS_NAME_LENGTH = 40;

	@GET
	@NonBlocking
	@Path("/css/{name}")
	@Produces(value = { "text/css", "application/json" })
	public Response getCss(@PathParam(value = "name") String name)
	{
		if (name.isBlank() || name.contains(File.separator) || name.length() > MAX_CSS_NAME_LENGTH)
		{
			return returnBadRequest("The requested CSS name is NOT valid! Provided name: " + name);
		}

		Matcher match = CSS_NAME_PATTERN.matcher(name);

		if (!match.find())
		{
			return returnBadRequest("The requested CSS name is NOT in proper format! Provided name: " + name);
		}

		StaticFile staticFile = Website.STATIC_CACHE.values().stream().filter(value -> {
			return value.getName().equals(name);
		}).findFirst().orElse(null);

		if (staticFile == null)
		{
			return returnBadRequest("Clinet tried to find a CSS file with an unknown name!");
		}

		String etag = staticFile.getHash();
		String lastModified = WebHelper.parseLastModifiedTime(staticFile.getModified());

		if (!WebHelper.hasResourceChanged(httpHeaders, etag, lastModified))
		{
			return Response.status(Status.NOT_MODIFIED).build();
		}

		String pathToStyle = cssDirectoryPath + File.separator + name;

		if (!Files.exists(Paths.get(pathToStyle), LinkOption.NOFOLLOW_LINKS))
		{
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok()
				.entity(Paths.get(pathToStyle))
				.header(HttpHeaders.ETAG, etag)
				.header(HttpHeaders.CACHE_CONTROL, cacheControl)
				.header(HttpHeaders.LAST_MODIFIED, lastModified)
				.build();
	}

	@GET
	@NonBlocking
	@Path("/image/{name}")
	@Produces(value = { "image/png", "image/jpeg", "image/gif", "image/x-icon", "application/json" })
	public Response getImage(@PathParam(value = "name") String name)
	{
		/**
		 * TODO: Image endpoint feature
		 * 
		 * Maybe add a nginx like path parameter which allows for fetching images witch
		 * a specific width and height ratio.
		 */

		if (name.isBlank() || name.contains(File.separator) || name.length() > MAX_IMAGE_NAME_LENGTH)
		{
			return returnBadRequest("The requested image name is NOT valid! Provided name: " + name);
		}

		Matcher match = IMAGE_NAME_PATTERN.matcher(name);

		if (!match.find())
		{
			return returnBadRequest("The requested image name is NOT in proper format! Provided name: " + name);
		}

		StaticFile staticFile = Website.STATIC_CACHE.values().stream().filter(value -> {
			return value.getName().equals(name);
		}).findFirst().orElse(null);

		if (staticFile == null)
		{
			return returnBadRequest("Clinet tried to find a image with an unknown name!");
		}

		String etag = staticFile.getHash();
		String lastModified = WebHelper.parseLastModifiedTime(staticFile.getModified());

		if (!WebHelper.hasResourceChanged(httpHeaders, etag, lastModified))
		{
			return Response.status(Status.NOT_MODIFIED).build();
		}

		String pathToImage = imagesDirectoryPath + File.separator + name;

		if (!Files.exists(Paths.get(pathToImage), LinkOption.NOFOLLOW_LINKS))
		{
			return Response.status(Status.NOT_FOUND).build();
		}

		return Response.ok()
				.entity(Paths.get(pathToImage))
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
	 * 
	 * @return A <b>BAD_REQUEST</b> {@link Response} instance with a
	 *         {@link JsonObject} entity.
	 */
	private static final Response returnBadRequest(String message)
	{
		JsonObject response = new JsonObject();

		response.put("message", message);

		return Response.status(Status.BAD_REQUEST).entity(response).build();
	}
}
