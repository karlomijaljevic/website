package xyz.mijaljevic.lifecycle;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;

class UniqueVisitTrackerTest {
    private static final Duration WINDOW = Duration.ofHours(24);

    @Test
    @DisplayName("the first request for a key counts as a unique visit")
    void isNewVisit_firstRequest_counts() {
        UniqueVisitTracker tracker = new UniqueVisitTracker(WINDOW, () -> 0L);

        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isTrue();
    }

    @Test
    @DisplayName("repeated requests within the window do not count again")
    void isNewVisit_repeatWithinWindow_doesNotCount() {
        AtomicLong clock = new AtomicLong(0L);
        UniqueVisitTracker tracker = new UniqueVisitTracker(WINDOW, clock::get);

        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isTrue();

        // Same visitor refreshing one hour later.
        clock.set(Duration.ofHours(1).toMillis());
        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isFalse();

        // And again, still inside the window.
        clock.set(Duration.ofHours(23).toMillis());
        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isFalse();
    }

    @Test
    @DisplayName("distinct visitor keys each count once")
    void isNewVisit_distinctKeys_eachCount() {
        UniqueVisitTracker tracker = new UniqueVisitTracker(WINDOW, () -> 0L);

        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isTrue();
        assertThat(tracker.isNewVisit("1.2.3.4|Chrome")).isTrue();
        assertThat(tracker.isNewVisit("5.6.7.8|Firefox")).isTrue();
    }

    @Test
    @DisplayName("a visitor returning after the window counts as a new unique visit")
    void isNewVisit_afterWindow_countsAgain() {
        AtomicLong clock = new AtomicLong(0L);
        UniqueVisitTracker tracker = new UniqueVisitTracker(WINDOW, clock::get);

        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isTrue();

        clock.set(WINDOW.toMillis());
        assertThat(tracker.isNewVisit("1.2.3.4|Firefox")).isTrue();
    }

    @Test
    @DisplayName("evictStale drops keys whose window has elapsed and keeps fresh ones")
    void evictStale_dropsExpiredKeysOnly() {
        AtomicLong clock = new AtomicLong(0L);
        UniqueVisitTracker tracker = new UniqueVisitTracker(WINDOW, clock::get);

        tracker.isNewVisit("old|Firefox");

        // A fresher visit, half the window later.
        clock.set(Duration.ofHours(12).toMillis());
        tracker.isNewVisit("fresh|Firefox");

        // Move to exactly the first key's window end; the second is still inside it.
        clock.set(WINDOW.toMillis());
        tracker.evictStale();

        // The evicted "old" key counts as new again, while the still fresh
        // "fresh" key (seen at hour 12, now hour 24) remains de-duplicated.
        assertThat(tracker.isNewVisit("old|Firefox")).isTrue();
        assertThat(tracker.isNewVisit("fresh|Firefox")).isFalse();
    }
}
