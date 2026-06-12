package xyz.mijaljevic.lifecycle;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/**
 * Application scoped, in-memory gate that decides whether a request should be
 * counted as a <i>unique</i> visit. Without it every page hit, static asset
 * fetch and browser refresh would be recorded by the {@link VisitorCounter},
 * heavily inflating the totals.
 *
 * <p>
 * A visit is considered unique per visitor key (typically the client IP plus
 * its <i>User-Agent</i>) within a configurable rolling window. The first
 * request from a key counts; further requests from the same key are ignored
 * until {@code application.visitors.unique-window} has elapsed since that first
 * request, after which the visitor counts once more (a returning visit).
 * </p>
 *
 * <p>
 * The map of seen keys lives only in memory: it is not persisted, so after a
 * restart every visitor is considered new again. Stale keys are swept
 * periodically by {@link #evictStale()} to keep the map bounded.
 * </p>
 */
@ApplicationScoped
public final class UniqueVisitTracker {
    /**
     * Maps a visitor key to the epoch millisecond timestamp of the visit that
     * opened its current window. Entries are short-lived and evicted once their
     * window elapses.
     */
    private final ConcurrentHashMap<String, Long> lastSeen = new ConcurrentHashMap<>();

    /**
     * Length of the de-duplication window in milliseconds. Requests from the
     * same key within this many milliseconds of the window-opening visit are
     * not counted again.
     */
    private final long windowMillis;

    /**
     * Source of the current time, in epoch milliseconds. Indirected so tests
     * can drive the clock deterministically.
     */
    private final LongSupplier clock;

    /**
     * Creates the tracker with its configured window, using the system clock.
     *
     * @param window The rolling de-duplication window. Parsed by the Quarkus
     *               {@link Duration} converter, so values like {@code 24h} or
     *               {@code PT24H} are both accepted.
     */
    @Inject
    @SuppressWarnings("unused")
    UniqueVisitTracker(
            @ConfigProperty(
                    name = "application.visitors.unique-window",
                    defaultValue = "24h"
            ) final Duration window
    ) {
        this(window, System::currentTimeMillis);
    }

    /**
     * Creates the tracker with an explicit clock. Intended for tests.
     *
     * @param window The rolling de-duplication window.
     * @param clock  Supplier of the current time in epoch milliseconds.
     */
    UniqueVisitTracker(@Nonnull final Duration window, @Nonnull final LongSupplier clock) {
        this.windowMillis = window.toMillis();
        this.clock = clock;
    }

    /**
     * Decides whether the supplied visitor key represents a new unique visit
     * and, if so, opens a fresh window for it. Safe to call concurrently: the
     * check-and-update is atomic per key.
     *
     * @param visitorKey The key identifying the visitor (e.g. client IP plus
     *                   <i>User-Agent</i>).
     * @return {@code true} when the key has not been seen within the window
     *         (so the visit should be counted), {@code false} otherwise.
     */
    public boolean isNewVisit(@Nonnull final String visitorKey) {
        final long now = clock.getAsLong();
        final boolean[] isNew = {false};

        lastSeen.compute(visitorKey, (key, openedAt) -> {
            Log.debugf("Visitor %s: opened at %s", key, openedAt);

            if (openedAt == null || now - openedAt >= windowMillis) {
                isNew[0] = true;
                return now;
            }

            return openedAt;
        });

        return isNew[0];
    }

    /**
     * Removes keys whose window has already elapsed. Such keys would count as
     * new on their next request anyway, so dropping them never changes the
     * outcome of {@link #isNewVisit(String)}; it only bounds the map size.
     * Scheduled rather than done inline to keep {@link #isNewVisit(String)}
     * cheap on the hot path.
     */
    @Scheduled(
            identity = "unique_visit_tracker_eviction",
            cron = "{application.visitors.cleanup-interval}",
            delayed = "1m"
    )
    void evictStale() {
        final long now = clock.getAsLong();
        final int before = lastSeen.size();

        lastSeen.entrySet().removeIf(entry -> now - entry.getValue() >= windowMillis);

        final int evicted = before - lastSeen.size();

        if (evicted > 0) {
            Log.debugf("Evicted %d stale visitor key(s); %d remaining.", evicted, lastSeen.size());
        }
    }
}
