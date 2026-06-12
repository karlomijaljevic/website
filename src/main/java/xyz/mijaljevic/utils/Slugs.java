package xyz.mijaljevic.utils;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utility class that turns a blog title into a URL-safe slug used as the public
 * blog identifier in {@code /blog/{slug}} URLs and RSS GUIDs.
 */
public final class Slugs {
    /**
     * Matches combining diacritical marks left after Unicode decomposition.
     */
    private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");

    /**
     * Matches any run of characters outside the {@code [a-z0-9]} range; each
     * run collapses to a single hyphen.
     */
    private static final Pattern NON_SLUG = Pattern.compile("[^a-z0-9]+");

    /**
     * Matches leading or trailing hyphens to be stripped from the slug.
     */
    private static final Pattern EDGE_HYPHENS = Pattern.compile("(^-+)|(-+$)");

    private Slugs() {
        // NOTE: Utility class, not meant to be instantiated.
    }

    /**
     * Derives a URL-safe slug from the provided title: strips diacritics,
     * lowercases, collapses every run of non-alphanumeric characters into a
     * single hyphen, and trims leading/trailing hyphens.
     *
     * @param title The blog title to slugify.
     * @return The derived slug, possibly empty if the title has no
     * alphanumeric content.
     * @throws NullPointerException if {@code title} is null.
     */
    public static String slugify(final String title) {
        Objects.requireNonNull(title, "title must not be null");

        final String decomposed = Normalizer.normalize(title, Normalizer.Form.NFD);
        final String ascii = DIACRITICS.matcher(decomposed).replaceAll("");
        final String lower = ascii.toLowerCase(Locale.ROOT);
        final String hyphenated = NON_SLUG.matcher(lower).replaceAll("-");

        return EDGE_HYPHENS.matcher(hyphenated).replaceAll("");
    }
}
