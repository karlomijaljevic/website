package xyz.mijaljevic.web.page;

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
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.dto.BlogLink;
import xyz.mijaljevic.web.WebHelper;
import xyz.mijaljevic.web.WebKeys;

import java.util.List;

/**
 * All blogs page. Displays all the blogs to the user.
 */
@PermitAll
@Path("/blogs")
public final class AllBlogsPage {
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    @Inject
    HttpHeaders httpHeaders;

    @Inject
    Template allBlogsPage;

    /**
     * Title of the all blogs page.
     */
    private static final String TITLE = "Blog";

    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getPage() {
        String eTag = WebHelper.getETag();
        String lastModified = WebHelper.getLastModified();

        if (WebHelper.isResourceNotChanged(httpHeaders, eTag, lastModified)) {
            return Response.status(Status.NOT_MODIFIED).build();
        }

        List<BlogLink> blogs = Website.BLOG_CACHE.values()
                .stream()
                .sorted()
                .map(BlogLink::generateBlogLinkFromBlog)
                .toList();

        TemplateInstance template = allBlogsPage.data(WebKeys.BLOGS, blogs)
                .data(WebKeys.TITLE, TITLE);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }
}
