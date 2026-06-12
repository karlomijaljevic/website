package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import java.time.ZoneId;

/**
 * Application class. Implements the {@link QuarkusApplication} interface.
 * Contains the application "<i>global</i>" constants.
 */
public final class Website implements QuarkusApplication {
    /**
     * Limit of latest blogs to display on the Home page and RSS feed.
     */
    public static final int NUMBER_OF_BLOGS_TO_DISPLAY = 8;

    /**
     * Time zone used by the website for clients.
     */
    public static final ZoneId TIME_ZONE = ZoneId.of("UTC");

    /**
     * Hash algorithm used by the website for file and Etag hashing.
     */
    public static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public int run(final String... args) {
        Quarkus.waitForExit();

        return 0;
    }
}
