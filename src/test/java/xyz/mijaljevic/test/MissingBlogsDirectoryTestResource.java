package xyz.mijaljevic.test;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Points {@code application.blogs-directory} at a path whose leaf directory
 * does <b>not</b> exist when the Quarkus application starts, reproducing the
 * Task 9 startup race scenario (a missing blogs directory). The application
 * must provision the directory at boot and start successfully rather than
 * {@code asyncExit}; the test asserts the directory is created and the home
 * page is served.
 */
public final class MissingBlogsDirectoryTestResource implements QuarkusTestResourceLifecycleManager {
    /**
     * The temporary parent directory created in {@link #start()}. Its
     * {@code blogs} child is intentionally left absent so the application has
     * to create it.
     */
    private Path parent;

    @Override
    public Map<String, String> start() {
        try {
            parent = Files.createTempDirectory("website-missing-blogs");
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to create temp parent directory", e);
        }

        // Intentionally NOT created: the application must provision it at boot.
        final Path absentBlogsDir = parent.resolve("blogs");

        return Map.of("application.blogs-directory", absentBlogsDir.toString());
    }

    @Override
    public void stop() {
        if (parent == null) {
            return;
        }

        try (Stream<Path> paths = Files.walk(parent)) {
            paths.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new UncheckedIOException("Failed to delete " + path, e);
                        }
                    });
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to clean up temp parent directory", e);
        }
    }
}
