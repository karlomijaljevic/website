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
