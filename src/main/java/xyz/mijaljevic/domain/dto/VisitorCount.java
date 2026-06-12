package xyz.mijaljevic.domain.dto;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * The visitor counts DTO.
 *
 * @param humans Number of humans which have visited the website.
 * @param crawlers Number of crawlers which have visited the website.
 * @param aiBots Number of AI bots which have visited the website.
 */
public record VisitorCount(long humans, long crawlers, long aiBots) {
    private static String fmt(final long v) {
        return NumberFormat.getInstance(Locale.US).format(v);
    }

    /**
     * @return Number of humans as a readable US formatter number.
     */
    public String humansFormatted() {
        return fmt(humans);
    }

    /**
     * @return Number of crawlers as a readable US formatter number.
     */
    public String crawlersFormatted() {
        return fmt(crawlers);
    }

    /**
     * @return Number of AI bots as a readable US formatter number.
     */
    public String aiBotsFormatted() {
        return fmt(aiBots);
    }
}
