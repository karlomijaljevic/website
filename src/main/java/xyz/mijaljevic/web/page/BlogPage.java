package xyz.mijaljevic.web.page;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.Blocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.orm.model.Blog;

/**
 * Displays a blog to the end user.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@PermitAll
@Path("/blog/{id}")
public final class BlogPage
{
	@ConfigProperty(name = "application.cache-control")
	private String cacheControl;

	@Inject
	private HttpHeaders httpHeaders;

	@Inject
	private Template blogPage;

	@PathParam(value = "id")
	private Long id;

	@GET
	@Blocking
	@Produces(MediaType.TEXT_HTML)
	public Response getPage()
	{
		Blog blog = Website.BLOG_CACHE.values().stream().filter(value -> {
			return value.getId().equals(id);
		}).findFirst().orElse(null);

		if (blog == null)
		{
			throw new NotFoundException("Clinet tried to find a blog with an unknown ID!");
		}

		LocalDateTime lastUpdated = blog.getUpdated() == null ? blog.getCreated() : blog.getUpdated();

		String etag = blog.getHash();
		String lastModified = PageHelper.parseLastModifiedTime(lastUpdated);

		if (!PageHelper.hasResourceChanged(httpHeaders, etag, lastModified))
		{
			Website.BLOG_CACHE.get(blog.getFileName()).setLastRead(LocalDateTime.now());

			return Response.status(Status.NOT_MODIFIED).build();
		}

		if (blog.getData() == null)
		{
			try
			{
				blog = syncBlogData(blog);
			}
			catch (IOException e)
			{
				Log.error("Failed to display the blog with id " + id, e);

				throw new IllegalStateException("Page rendering for blog with ID " + id + " failed!");
			}
		}

		Website.BLOG_CACHE.get(blog.getFileName()).setLastRead(LocalDateTime.now());

		TemplateInstance template = blogPage.data(PageKeys.BLOG, blog).data(PageKeys.TITLE, blog.getTitle());

		return Response.ok()
				.entity(template)
				.header(HttpHeaders.ETAG, etag)
				.header(HttpHeaders.CACHE_CONTROL, cacheControl)
				.header(HttpHeaders.LAST_MODIFIED, lastModified)
				.build();
	}

	/**
	 * Reads the data from the blog file and puts the updated blog reference back
	 * into the blogs cache updating the data.
	 * 
	 * @param blog The {@link Blog} entity that has been requested.
	 * 
	 * @return Returns the updated {@link Blog} reference.
	 * 
	 * @throws IOException In case it failed to read the blog data from the file.
	 */
	private static final synchronized Blog syncBlogData(Blog blog) throws IOException
	{
		// Check if it has been synced by a close called thread
		Blog synced = Website.BLOG_CACHE.get(blog.getFileName());

		if (synced.getData() != null)
		{
			return synced;
		}

		String filePath = Website.BlogsDirectory.getPath() + File.separator + blog.getFileName();

		blog.setData(Files.readString(Paths.get(filePath), StandardCharsets.UTF_8));

		Website.BLOG_CACHE.put(blog.getFileName(), blog);

		return blog;
	}
}
