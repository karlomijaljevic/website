package xyz.mijaljevic.web.page;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.smallrye.common.annotation.NonBlocking;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.dto.BlogLink;
import xyz.mijaljevic.web.WebHelper;
import xyz.mijaljevic.web.WebKeys;

import java.util.List;

/**
 * Topic page. Displays all the blogs that contain the specified topic.
 */
@PermitAll
@Path("/topic/{name}")
public final class TopicPage {
    /**
     * Cache control header value.
     */
    @ConfigProperty(name = "application.cache-control")
    String cacheControl;

    /**
     * HTTP headers.
     */
    @Inject
    HttpHeaders httpHeaders;

    /**
     * Template for the topic page.
     */
    @Inject
    Template topicPage;

    /**
     * Name of the topic to display.
     */
    @PathParam("name")
    String name;

    /**
     * Title of the topics page.
     */
    private static final String TITLE = "Topic";

    /**
     * Returns the topic page with all blogs that contain the specified topic.
     *
     * @return Response containing the topic page
     */
    @GET
    @NonBlocking
    @Produces(MediaType.TEXT_HTML)
    public Response getPage() {
        String eTag = WebHelper.getETag();
        String lastModified = WebHelper.getLastModified();

        if (WebHelper.isResourceNotChanged(httpHeaders, eTag, lastModified)) {
            return Response.status(Response.Status.NOT_MODIFIED).build();
        }

        List<BlogLink> blogs = Website.BLOG_CACHE
                .values()
                .stream()
                .filter(blog -> blog.getBlogTopics()
                        .stream()
                        .anyMatch(blogTopic -> blogTopic.getTopic()
                                .getName()
                                .equalsIgnoreCase(name)
                        )
                )
                .sorted()
                .map(BlogLink::generateBlogLinkFromBlog)
                .toList()
                .reversed();

        TemplateInstance template = topicPage.data(WebKeys.BLOGS, blogs)
                .data(WebKeys.TITLE, TITLE + " - " + name)
                .data(WebKeys.TOPIC, name);

        return Response.ok()
                .entity(template)
                .header(HttpHeaders.ETAG, eTag)
                .header(HttpHeaders.CACHE_CONTROL, cacheControl)
                .header(HttpHeaders.LAST_MODIFIED, lastModified)
                .build();
    }
}
