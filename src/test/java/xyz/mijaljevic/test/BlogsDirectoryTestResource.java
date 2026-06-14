package xyz.mijaljevic.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Seeds a throwaway blogs directory with known markdown files <b>before</b> the
 * Quarkus application starts and points {@code application.blogs-directory} at
 * it, so the {@code @QuarkusTest} integration tests have a deterministic, known
 * set of blogs to exercise (the real {@code blogs/} directory is gitignored and
 * absent on CI, where the blog scheduler would otherwise {@code asyncExit} the
 * app at boot).
 *
 * <p>
 * The seeded titles, slugs and bodies are the contract the integration tests
 * assert against; keep them in sync with the constants below.
 * </p>
 */
public final class BlogsDirectoryTestResource implements QuarkusTestResourceLifecycleManager {
    /**
     * Title of the first seeded blog.
     */
    public static final String ALPHA_TITLE = "Alpha Post";

    /**
     * Slug derived from {@link #ALPHA_TITLE}.
     */
    public static final String ALPHA_SLUG = "alpha-post";

    /**
     * File name of the first seeded blog.
     */
    public static final String ALPHA_FILE = "alpha.md";

    /**
     * Author declared in the first seeded blog's front-matter metadata.
     */
    public static final String ALPHA_AUTHOR = "Test Author";

    /**
     * Created timestamp derived from the first seeded blog's {@code Date}
     * front-matter metadata ({@code 2-Jan-2020}), at start of day.
     */
    public static final LocalDateTime ALPHA_CREATED = LocalDateTime.of(2020, 1, 2, 0, 0);

    /**
     * Updated timestamp derived from the first seeded blog's {@code Updated}
     * front-matter metadata ({@code 5-Mar-2020}), at start of day.
     */
    public static final LocalDateTime ALPHA_UPDATED = LocalDateTime.of(2020, 3, 5, 0, 0);

    /**
     * Title of the second seeded blog.
     */
    public static final String BETA_TITLE = "Beta Post";

    /**
     * Slug derived from {@link #BETA_TITLE}.
     */
    public static final String BETA_SLUG = "beta-post";

    /**
     * File name of the second seeded blog.
     */
    public static final String BETA_FILE = "beta.md";

    /**
     * Title of the third seeded blog.
     */
    public static final String GAMMA_TITLE = "Gamma Post";

    /**
     * Slug derived from {@link #GAMMA_TITLE}.
     */
    public static final String GAMMA_SLUG = "gamma-post";

    /**
     * File name of the third seeded blog.
     */
    public static final String GAMMA_FILE = "gamma.md";

    /**
     * Created timestamp derived from the second seeded blog's {@code Date}
     * front-matter metadata ({@code 15-Jun-2021}), at start of day. Beta carries
     * no {@code Title}, so its title still falls back to the heading.
     */
    public static final LocalDateTime BETA_CREATED = LocalDateTime.of(2021, 6, 15, 0, 0);

    /**
     * Number of blogs seeded into the temporary directory.
     */
    public static final int SEEDED_BLOG_COUNT = 3;

    /**
     * The temporary blogs directory created in {@link #start()}.
     */
    private Path blogsDir;

    @Override
    public Map<String, String> start() {
        try {
            blogsDir = Files.createTempDirectory("website-test-blogs");

            // Alpha carries a full front-matter block: its title and created
            // date are driven by the metadata, not the heading or filesystem.
            write(ALPHA_FILE, """
                    ---
                    Title: %s
                    Author: %s
                    Date: 2-Jan-2020
                    Updated: 5-Mar-2020
                    Tags: alpha, test
                    ---
                    # Heading ignored in favour of metadata title

                    The **alpha** body with a [link](https://example.com).
                    """.formatted(ALPHA_TITLE, ALPHA_AUTHOR));
            // Beta carries only a Date (the created timestamp has no filesystem
            // fallback): its title still falls back to the heading and its
            // author/tags stay empty.
            write(BETA_FILE, """
                    ---
                    Date: 15-Jun-2021
                    ---
                    # %s

                    The beta body & it carries an ampersand.
                    """.formatted(BETA_TITLE));
            // Gamma likewise carries only a Date.
            write(GAMMA_FILE, """
                    ---
                    Date: 20-Sep-2022
                    ---
                    # %s

                    The gamma body with `inline code`.
                    """.formatted(GAMMA_TITLE));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to seed test blogs directory", e);
        }

        return Map.of("application.blogs-directory", blogsDir.toString());
    }

    @Override
    public void stop() {
        if (blogsDir == null) {
            return;
        }

        try (Stream<Path> paths = Files.walk(blogsDir)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to delete " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clean up test blogs directory", e);
        }
    }

    /**
     * Writes a seeded markdown file into the temporary blogs directory.
     *
     * @param fileName The file name to create.
     * @param content  The markdown content to write.
     * @throws IOException If the file cannot be written.
     */
    private void write(final String fileName, final String content) throws IOException {
        Files.writeString(blogsDir.resolve(fileName), content, StandardCharsets.UTF_8);
    }
}
