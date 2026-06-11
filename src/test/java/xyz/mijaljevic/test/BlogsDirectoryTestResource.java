/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */

package xyz.mijaljevic.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

            write(ALPHA_FILE, "# " + ALPHA_TITLE + "\n\nThe **alpha** body with a [link](https://example.com).\n");
            write(BETA_FILE, "# " + BETA_TITLE + "\n\nThe beta body & it carries an ampersand.\n");
            write(GAMMA_FILE, "# " + GAMMA_TITLE + "\n\nThe gamma body with `inline code`.\n");
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
