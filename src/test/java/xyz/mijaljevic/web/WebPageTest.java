package xyz.mijaljevic.web;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.test.BlogsDirectoryTestResource;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;

/**
 * Integration tests for {@link WebPage}: the public HTML pages, their HTTP
 * caching headers (ETag / Last-Modified with conditional requests), unknown
 * slug handling, and the error page header regression from Task 1.
 */
@QuarkusTest
@QuarkusTestResource(value = BlogsDirectoryTestResource.class, restrictToAnnotatedClass = true)
class WebPageTest {
    /**
     * The {@code Cache-Control} value configured in {@code application.properties}.
     */
    private static final String EXPECTED_CACHE_CONTROL = "public, max-age=0, must-revalidate";

    /**
     * A SHA-256 hash rendered as a lowercase hex string. Up to 64 characters:
     * the blog hash is not zero-padded, so a leading-zero byte yields a shorter
     * string. Crucially it never matches the old {@code AtomicReference@hash}
     * bug output (Task 1), which carried uppercase letters and an {@code @}.
     */
    private static final String HEX_HASH = "[0-9a-f]{1,64}";

    @Test
    @DisplayName("GET / serves the home page with caching headers")
    void getHome_returnsOkWithCachingHeaders() {
        given()
                .when().get("/")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .header("ETag", matchesHex())
                .header("Cache-Control", EXPECTED_CACHE_CONTROL)
                .header("Last-Modified", org.hamcrest.Matchers.notNullValue())
                .body(containsString("Latest blogs"));
    }

    @Test
    @DisplayName("GET /blogs lists every seeded blog")
    void getBlogs_listsSeededBlogs() {
        given()
                .when().get("/blogs")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .body(containsString(BlogsDirectoryTestResource.ALPHA_TITLE))
                .body(containsString(BlogsDirectoryTestResource.BETA_TITLE))
                .body(containsString(BlogsDirectoryTestResource.GAMMA_TITLE));
    }

    @Test
    @DisplayName("GET /contact serves the contact page")
    void getContact_returnsOk() {
        given()
                .when().get("/contact")
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"));
    }

    @Test
    @DisplayName("GET /blog/{slug} renders the blog body for a known slug")
    void getBlogBySlug_returnsRenderedBody() {
        given()
                .when().get("/blog/" + BlogsDirectoryTestResource.ALPHA_SLUG)
                .then()
                .statusCode(200)
                .contentType(containsString("text/html"))
                .header("ETag", matchesHex())
                .header("Cache-Control", EXPECTED_CACHE_CONTROL)
                // The rendered markdown body and the link attribute provider output.
                .body(containsString("alpha"))
                .body(containsString("target=\"_blank\""));
    }

    @Test
    @DisplayName("GET /blog/{slug} for an unknown slug redirects to the not-found error page")
    void getBlogBySlug_unknown_redirectsToNotFound() {
        given()
                .redirects().follow(false)
                .when().get("/blog/does-not-exist")
                .then()
                .statusCode(303)
                .header("Location", containsString("/error/not-found"));
    }

    @Test
    @DisplayName("A matching If-None-Match returns 304 Not Modified")
    void conditionalRequest_ifNoneMatch_returns304() {
        String etag = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().header("ETag");

        given()
                .header("If-None-Match", etag)
                .when().get("/")
                .then()
                .statusCode(304);
    }

    @Test
    @DisplayName("A matching If-Modified-Since returns 304 Not Modified")
    void conditionalRequest_ifModifiedSince_returns304() {
        String lastModified = given()
                .when().get("/")
                .then().statusCode(200)
                .extract().header("Last-Modified");

        given()
                .header("If-Modified-Since", lastModified)
                .when().get("/")
                .then()
                .statusCode(304);
    }

    @Test
    @DisplayName("The ETag is stable across repeated requests to the same page")
    void eTag_isStableAcrossRequests() {
        String first = given()
                .when().get("/blog/" + BlogsDirectoryTestResource.ALPHA_SLUG)
                .then().statusCode(200)
                .extract().header("ETag");

        String second = given()
                .when().get("/blog/" + BlogsDirectoryTestResource.ALPHA_SLUG)
                .then().statusCode(200)
                .extract().header("ETag");

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("The error page serves real hash/date header values, not AtomicReference toString (Task 1 regression)")
    void errorPage_servesProperHeaderValues() {
        String etag = given()
                .when().get("/error/not-found")
                .then()
                .statusCode(200)
                .header("Cache-Control", EXPECTED_CACHE_CONTROL)
                .header("Last-Modified", org.hamcrest.Matchers.notNullValue())
                .extract().header("ETag");

        // The bug served the AtomicReference object itself; assert the header is
        // the computed hex hash and carries no class-identity fingerprint.
        assertThat(etag)
                .matches(HEX_HASH)
                .doesNotContain("AtomicReference")
                .doesNotContain("@");
    }

    /**
     * @return A Hamcrest matcher asserting a header is a 64-char hex SHA-256 hash.
     */
    private static org.hamcrest.Matcher<String> matchesHex() {
        return org.hamcrest.Matchers.matchesRegex(HEX_HASH);
    }
}
