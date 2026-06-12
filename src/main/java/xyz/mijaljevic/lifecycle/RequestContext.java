package xyz.mijaljevic.lifecycle;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import xyz.mijaljevic.Website;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Request scoped bean that captures the incoming request {@link HttpHeaders}
 * and exposes the HTTP caching utilities shared by the web resources
 * ({@code WebPage}, {@code StaticFileServer}, {@code RssFeed}): generating
 * <i>ETag</i>/<i>Last-Modified</i> values and resolving conditional requests
 * against them.
 */
@RequestScoped
public final class RequestContext {
    /**
     * The incoming request HTTP headers, used for conditional-request
     * comparisons.
     */
    private final HttpHeaders httpHeaders;

    /**
     * Creates the context with the incoming request headers.
     *
     * @param httpHeaders The incoming request {@link HttpHeaders}.
     */
    @Inject
    public RequestContext(final HttpHeaders httpHeaders) {
        this.httpHeaders = httpHeaders;
    }

    /**
     * HTTP <i>Last-Modified</i> date format.
     */
    private static final DateTimeFormatter LM_FORMAT = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz");

    /**
     * Converts the provided {@link LocalDateTime} into an HTTP
     * <i>Last-Modified</i> header value.
     *
     * @param lastModifiedTime A {@link LocalDateTime} instance.
     * @return An HTTP <i>Last-Modified</i> header {@link String}.
     */
    @Nonnull
    public static String parseLastModifiedTime(
            @Nonnull final LocalDateTime lastModifiedTime
    ) {
        return lastModifiedTime.atZone(Website.TIME_ZONE).format(LM_FORMAT);
    }

    /**
     * Generates an <i>ETag</i> hash from the provided string.
     *
     * @param string A {@link String} to turn into an ETag hash.
     * @return Returns a hexadecimal hash from the provided string.
     * @throws NullPointerException if {@code string} is null.
     */
    public static @Nonnull String generateEtagHash(final String string) {
        Objects.requireNonNull(string, "string for E_TAG hash must not be null");

        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(Website.HASH_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            Log.fatalf(e, "Missing hashing algorithm: %s", Website.HASH_ALGORITHM);
            Quarkus.asyncExit();
        }

        if (digest == null) {
            throw new NullPointerException("Digest for E_TAG hash is null!");
        }

        final byte[] hash = digest.digest(string.getBytes(StandardCharsets.UTF_8));

        final StringBuilder hexString = new StringBuilder(2 * hash.length);

        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);

            if (hex.length() == 1) {
                hexString.append('0');
            }

            hexString.append(hex);
        }

        return hexString.toString();
    }

    /**
     * Compares the provided <i>ETag</i> with the <i>If-None-Match</i> header
     * and the provided <i>Last-Modified</i> with the <i>If-Modified-Since</i>
     * header of the captured request.
     *
     * <p>
     * Priority is given to the <i>ETag</i> and <i>If-None-Match</i>
     * comparison.
     * </p>
     *
     * @param etag         The HTTP ETag header that the resource is currently
     *                     serving.
     * @param lastModified The HTTP Last-Modified header that the resource is
     *                     currently serving.
     * @return A {@link Response.Status#NOT_MODIFIED} response if the resource
     * was not modified since the last visit, or {@code null} otherwise.
     */
    @Nullable
    public Response notModified(final String etag, final String lastModified) {
        final String ifNoneMatch = httpHeaders.getHeaderString(HttpHeaders.IF_NONE_MATCH);
        final String ifModifiedSince = httpHeaders.getHeaderString(HttpHeaders.IF_MODIFIED_SINCE);

        final boolean same;

        if (ifNoneMatch != null) {
            same = ifNoneMatch.equals(etag);
        } else if (ifModifiedSince != null) {
            same = ifModifiedSince.equals(lastModified);
        } else {
            same = false;
        }

        return same ? Response.status(Response.Status.NOT_MODIFIED).build() : null;
    }
}
