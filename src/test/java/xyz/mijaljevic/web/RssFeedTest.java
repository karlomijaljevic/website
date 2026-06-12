package xyz.mijaljevic.web;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import xyz.mijaljevic.test.BlogsDirectoryTestResource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for {@link RssFeed}: the rendered feed must be well-formed
 * RSS 2.0 XML (a golden-file guard for Task 5), every seeded blog must appear as
 * a complete item with a CDATA description carrying the rendered HTML, and the
 * conditional {@code If-None-Match} request must yield a 304.
 */
@QuarkusTest
@QuarkusTestResource(value = BlogsDirectoryTestResource.class, restrictToAnnotatedClass = true)
class RssFeedTest {
    @Test
    @DisplayName("GET /rss serves a well-formed RSS 2.0 feed with one item per seeded blog")
    void getRss_servesWellFormedFeed() throws Exception {
        String body = given()
                .when().get("/rss")
                .then()
                .statusCode(200)
                .contentType(containsString("text/xml"))
                .header("ETag", org.hamcrest.Matchers.notNullValue())
                .header("Cache-Control", "public, max-age=0, must-revalidate")
                .header("Last-Modified", org.hamcrest.Matchers.notNullValue())
                .extract().body().asString();

        // A successful parse is itself the escaping guard: malformed entities or
        // an unterminated CDATA section would throw here.
        Document document = parse(body);
        Element rss = document.getDocumentElement();

        assertThat(rss.getTagName()).isEqualTo("rss");
        assertThat(rss.getAttribute("version")).isEqualTo("2.0");

        Element channel = (Element) rss.getElementsByTagName("channel").item(0);
        assertThat(textOf(channel, "title")).isEqualTo("Karlo Mijaljević");

        NodeList items = channel.getElementsByTagName("item");
        assertThat(items.getLength()).isEqualTo(BlogsDirectoryTestResource.SEEDED_BLOG_COUNT);

        List<String> titles = new ArrayList<>();
        List<String> links = new ArrayList<>();
        StringBuilder descriptions = new StringBuilder();

        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            assertThat(textOf(item, "title")).isNotBlank();
            assertThat(textOf(item, "link")).isNotBlank();
            assertThat(textOf(item, "guid")).isNotBlank();
            assertThat(textOf(item, "pubDate")).isNotBlank();
            // The link and the guid are intentionally the same public blog URL.
            assertThat(textOf(item, "guid")).isEqualTo(textOf(item, "link"));

            titles.add(textOf(item, "title"));
            links.add(textOf(item, "link"));
            descriptions.append(textOf(item, "description"));
        }

        assertThat(titles).containsExactlyInAnyOrder(
                BlogsDirectoryTestResource.ALPHA_TITLE,
                BlogsDirectoryTestResource.BETA_TITLE,
                BlogsDirectoryTestResource.GAMMA_TITLE
        );

        assertThat(links).contains(
                "https://mijaljevic.xyz/blog/" + BlogsDirectoryTestResource.ALPHA_SLUG,
                "https://mijaljevic.xyz/blog/" + BlogsDirectoryTestResource.BETA_SLUG,
                "https://mijaljevic.xyz/blog/" + BlogsDirectoryTestResource.GAMMA_SLUG
        );

        // The description CDATA carries the rendered blog HTML; the alpha post
        // links out, so its link attribute provider output must be present.
        assertThat(descriptions.toString()).contains("target=\"_blank\"");
    }

    @Test
    @DisplayName("A matching If-None-Match returns 304 Not Modified for the feed")
    void getRss_ifNoneMatch_returns304() {
        String etag = given()
                .when().get("/rss")
                .then().statusCode(200)
                .extract().header("ETag");

        given()
                .header("If-None-Match", etag)
                .when().get("/rss")
                .then()
                .statusCode(304);
    }

    /**
     * Parses the provided XML into a DOM document, failing the test if the XML
     * is not well-formed.
     *
     * @param xml The XML document to parse.
     * @return The parsed {@link Document}.
     * @throws Exception If the XML cannot be parsed.
     */
    private static Document parse(final String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (ByteArrayInputStream input =
                     new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return builder.parse(input);
        }
    }

    /**
     * @param parent  The parent element.
     * @param tagName The direct child tag name to read.
     * @return The trimmed text content of the first matching child element.
     */
    private static String textOf(final Element parent, final String tagName) {
        return parent.getElementsByTagName(tagName).item(0).getTextContent().trim();
    }
}
