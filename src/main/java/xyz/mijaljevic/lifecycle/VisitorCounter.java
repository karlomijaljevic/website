package xyz.mijaljevic.lifecycle;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Shutdown;
import io.quarkus.runtime.Startup;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.domain.dto.VisitorCount;
import xyz.mijaljevic.domain.entity.VisitorType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Application scoped, in-memory counter of website visitors split by
 * {@link VisitorType}. The {@code VisitorLoggingFilter} feeds it one
 * {@link #record(VisitorType)} call per request and the
 * {@link GlobalTemplateData} exposes a {@link #snapshot()} to the footer
 * template.
 *
 * <p>
 * The counts survive restarts: they are read from a properties file at
 * {@link #load() startup} and written back to it at {@link #persist()
 * shutdown}. The file location is configured via
 * {@code application.visitors-file}.
 * </p>
 */
@ApplicationScoped
public final class VisitorCounter {
    /**
     * Properties key under which the human count is persisted.
     */
    private static final String HUMANS_KEY = "humans";

    /**
     * Properties key under which the crawler count is persisted.
     */
    private static final String CRAWLERS_KEY = "crawlers";

    /**
     * Properties key under which the AI bot count is persisted.
     */
    private static final String AI_BOTS_KEY = "aiBots";

    /**
     * Resolved path of the file that backs the counts across restarts.
     */
    private final Path countsFile;

    /**
     * Running number of requests classified as {@link VisitorType#HUMAN}.
     */
    private final AtomicLong humans = new AtomicLong();

    /**
     * Running number of requests classified as {@link VisitorType#CRAWLER}.
     */
    private final AtomicLong crawlers = new AtomicLong();

    /**
     * Running number of requests classified as {@link VisitorType#AI_BOT}.
     */
    private final AtomicLong aiBots = new AtomicLong();

    /**
     * Creates the counter with its configured backing file path.
     *
     * @param countsFilePath The path to the file that persists the counts.
     */
    @Inject
    VisitorCounter(
            @ConfigProperty(
                    name = "application.visitors-file",
                    defaultValue = "data/visitors.properties"
            ) final String countsFilePath
    ) {
        this.countsFile = Path.of(countsFilePath);
    }

    /**
     * Reads the persisted counts from {@link #countsFile} into memory. A
     * missing or unreadable file is not fatal: the counter simply starts from
     * zero. Runs once, eagerly, at application startup.
     */
    @Startup
    @SuppressWarnings("unused")
    void load() {
        if (!Files.exists(countsFile)) {
            Log.infof("Visitor counts file '%s' does not exist yet; starting from zero.", countsFile);
            return;
        }

        final Properties properties = new Properties();

        try (InputStream in = Files.newInputStream(countsFile)) {
            properties.load(in);
        } catch (IOException e) {
            Log.errorf(e, "Failed to read visitor counts file '%s'; starting from zero.", countsFile);
            return;
        }

        humans.set(parse(properties, HUMANS_KEY));
        crawlers.set(parse(properties, CRAWLERS_KEY));
        aiBots.set(parse(properties, AI_BOTS_KEY));

        Log.infof(
                "Loaded visitor counts: humans=%d crawlers=%d aiBots=%d",
                humans.get(),
                crawlers.get(),
                aiBots.get()
        );
    }

    /**
     * Writes the current counts to {@link #countsFile}, creating the parent
     * directory if needed. Runs once, at application shutdown. A failure to
     * write is logged but not propagated, so it cannot block shutdown.
     */
    @Shutdown
    @SuppressWarnings("unused")
    void persist() {
        final Properties properties = new Properties();
        properties.setProperty(HUMANS_KEY, Long.toString(humans.get()));
        properties.setProperty(CRAWLERS_KEY, Long.toString(crawlers.get()));
        properties.setProperty(AI_BOTS_KEY, Long.toString(aiBots.get()));

        try {
            final Path parent = countsFile.toAbsolutePath().getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream out = Files.newOutputStream(countsFile)) {
                properties.store(out, "Website visitor counts");
            }

            Log.infof(
                    "Persisted visitor counts: humans=%d crawlers=%d aiBots=%d",
                    humans.get(),
                    crawlers.get(),
                    aiBots.get()
            );
        } catch (IOException e) {
            Log.errorf(e, "Failed to persist visitor counts to '%s'.", countsFile);
        }
    }

    /**
     * Records a single visit of the supplied {@link VisitorType}, incrementing
     * the matching counter. Safe to call concurrently.
     *
     * @param visitorType The classification of the visit to record.
     */
    public void record(@Nonnull final VisitorType visitorType) {
        switch (visitorType) {
            case HUMAN -> humans.incrementAndGet();
            case CRAWLER -> crawlers.incrementAndGet();
            case AI_BOT -> aiBots.incrementAndGet();
        }
    }

    /**
     * Takes a consistent point-in-time copy of the current counts.
     *
     * @return A {@link VisitorCount} holding the current counts.
     */
    @Nonnull
    public VisitorCount snapshot() {
        return new VisitorCount(humans.get(), crawlers.get(), aiBots.get());
    }

    /**
     * Parses the {@code long} value stored under the supplied key, defaulting
     * to zero when the value is absent or malformed.
     *
     * @param properties The loaded properties.
     * @param key        The key whose value to parse.
     * @return The parsed count, or zero when absent or unparsable.
     */
    private static long parse(
            @Nonnull final Properties properties,
            final String key
    ) {
        final String value = properties.getProperty(key);

        if (value == null) {
            return 0L;
        }

        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            Log.warnf(
                    "Invalid visitor count for '%s': '%s'; defaulting to zero.",
                    key,
                    value
            );
            return 0L;
        }
    }
}
