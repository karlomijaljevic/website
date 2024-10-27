package xyz.mijaljevic.web.page;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;

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
import xyz.mijaljevic.Website;
import xyz.mijaljevic.orm.model.BlogLink;

/**
 * All blogs page. Displays all of the blogs to the user.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@PermitAll
@Path("/blogs")
public final class AllBlogsPage
{
	@ConfigProperty(name = "application.cache-control")
	private String cacheControl;

	@Inject
	private HttpHeaders httpHeaders;

	@Inject
	private Template allBlogsPage;

	/**
	 * Title of the all blogs page.
	 */
	private static final String TITLE = "Blog";

	@GET
	@NonBlocking
	@Produces(MediaType.TEXT_HTML)
	public Response getPage()
	{
		String eTag = PageHelper.getETag();
		String lastModified = PageHelper.getLastModified();

		if (!PageHelper.hasResourceChanged(httpHeaders, eTag, lastModified))
		{
			return Response.status(Status.NOT_MODIFIED).build();
		}

		List<BlogLink> blogs = new ArrayList<BlogLink>();

		Website.BLOG_CACHE.values().stream().sorted().forEach(blog -> {
			BlogLink blogLink = new BlogLink();

			blogLink.setId(blog.getId());
			blogLink.setTitle(blog.getTitle());
			blogLink.setDate(blog.parseCreated());

			blogs.add(blogLink);
		});

		TemplateInstance template = allBlogsPage.data(PageKeys.BLOGS, blogs).data(PageKeys.TITLE, TITLE);

		return Response.ok()
				.entity(template)
				.header(HttpHeaders.ETAG, eTag)
				.header(HttpHeaders.CACHE_CONTROL, cacheControl)
				.header(HttpHeaders.LAST_MODIFIED, lastModified)
				.build();
	}
}
