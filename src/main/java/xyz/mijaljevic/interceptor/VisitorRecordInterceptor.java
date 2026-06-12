package xyz.mijaljevic.interceptor;

import io.quarkus.logging.Log;
import io.vertx.core.net.SocketAddress;
import io.vertx.ext.web.RoutingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.ext.Provider;
import xyz.mijaljevic.domain.entity.VisitorType;
import xyz.mijaljevic.lifecycle.UniqueVisitTracker;
import xyz.mijaljevic.lifecycle.VisitorCounter;
import xyz.mijaljevic.utils.VisitorClassifier;

/**
 * Logs every incoming request together with a {@link VisitorType}
 * classification derived from its <i>User-Agent</i> header. The classification
 * is fed to the {@link VisitorCounter} only for <i>unique</i> visits, as
 * decided by the {@link UniqueVisitTracker}, so that repeated page hits, static
 * asset fetches and refreshes from the same visitor are not double counted.
 * This splits traffic into <i>humans</i>, <i>crawlers</i> and <i>AI bots</i>
 * both in the application log and in the running counts shown in the footer.
 */
@Provider
public final class VisitorRecordInterceptor implements ContainerRequestFilter {
    /**
     * Running counts of visitors, fed one record per unique visit.
     */
    private final VisitorCounter visitorCounter;

    /**
     * Decides whether a given request is a new unique visit.
     */
    private final UniqueVisitTracker uniqueVisitTracker;

    /**
     * Current request context, used to resolve the client address.
     */
    private final RoutingContext routingContext;

    /**
     * Creates the filter with its collaborating beans.
     *
     * @param visitorCounter     The {@link VisitorCounter} to record unique
     *                           visits into.
     * @param uniqueVisitTracker The {@link UniqueVisitTracker} gating which
     *                           requests count as visits.
     * @param routingContext     The current request's {@link RoutingContext}.
     */
    @Inject
    public VisitorRecordInterceptor(
            final VisitorCounter visitorCounter,
            final UniqueVisitTracker uniqueVisitTracker,
            final RoutingContext routingContext
    ) {
        this.visitorCounter = visitorCounter;
        this.uniqueVisitTracker = uniqueVisitTracker;
        this.routingContext = routingContext;
    }

    @Override
    public void filter(@Nonnull final ContainerRequestContext requestContext) {
        final String userAgent = requestContext.getHeaderString(HttpHeaders.USER_AGENT);

        final VisitorType visitorType = VisitorClassifier.classify(userAgent);

        final boolean unique = uniqueVisitTracker.isNewVisit(buildVisitorKey(userAgent));

        if (unique) {
            visitorCounter.record(visitorType);
        }

        Log.debugf(
                "VISITOR type=%s unique=%s method=%s path=%s userAgent=%s",
                visitorType,
                unique,
                requestContext.getMethod(),
                requestContext.getUriInfo().getPath(),
                userAgent);
    }

    /**
     * Builds the key that identifies a visitor for de-duplication: the client
     * IP address combined with its <i>User-Agent</i>. Two requests sharing both
     * are treated as the same visitor.
     *
     * @param userAgent The request <i>User-Agent</i>, may be {@code null}.
     * @return The visitor key, never {@code null}.
     */
    @Nonnull
    private String buildVisitorKey(final String userAgent) {
        final String clientIp = resolveClientIp();

        return (clientIp == null ? "unknown" : clientIp) + '|' + (userAgent == null ? "" : userAgent);
    }

    /**
     * Resolves the originating client IP from the request's resolved remote
     * address. With {@code quarkus.http.proxy.proxy-address-forwarding}
     * enabled, Quarkus already rewrites this address from the forwarding
     * headers of <i>trusted</i> proxies only (see
     * {@code quarkus.http.proxy.trusted-proxies}), so we deliberately do not
     * parse {@code X-Forwarded-For} ourselves: doing so would honour the header
     * unconditionally and let clients spoof it.
     *
     * @return The client IP, or {@code null} when it cannot be determined.
     */
    @Nullable
    private String resolveClientIp() {
        final SocketAddress remoteAddress = routingContext.request().remoteAddress();

        return remoteAddress == null ? null : remoteAddress.hostAddress();
    }
}
