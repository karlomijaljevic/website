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
import xyz.mijaljevic.model.dto.BlogLink;
import xyz.mijaljevic.web.WebHelper;
import xyz.mijaljevic.web.WebKeys;

/**
 * Home page of the website.
 */
@Path("/")
@PermitAll
public final class HomePage {
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    Template homePage;

    /**
     * HTML title of the home page.
     */
    private static final String TITLE = "Karlo Mijaljević";

    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getPage() {
        String eTag = WebHelper.getETag();
        String lastModified = WebHelper.getLastModified();

        if (WebHelper.isResourceNotChanged(httpHeaders, eTag, lastModified)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        List<BlogLink> blogs = new ArrayList<>();

        Website.retrieveRecentBlogs().forEach(blog -> blogs.add(BlogLink.generateBlogLinkFromBlog(blog)));

        TemplateInstance template = homePage.data(WebKeys.TITLE, TITLE).data(WebKeys.BLOGS, blogs);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }
}
