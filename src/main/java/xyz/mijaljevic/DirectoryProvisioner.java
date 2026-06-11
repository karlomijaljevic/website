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

package xyz.mijaljevic;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.nio.file.Path;

/**
 * Provisions the blogs and images directories at application startup and
 * exposes their resolved {@link Path}s.
 *
 * <p>
 * The directories are created in {@link #provision()} (a {@code @PostConstruct}
 * callback). The schedulers that watch these directories
 * ({@code BlogScheduler}, {@code ImageScheduler}) depend on this bean and read
 * their watched {@link Path} from it via {@link #blogsDirectory()} /
 * {@link #imagesDirectory()}. Because CDI constructs (and runs the
 * {@code @PostConstruct} of) a dependency before the first method call from a
 * dependent bean, the watched directory is guaranteed to exist before a
 * scheduler registers its {@code WatchService} on it. This removes the
 * startup race that previously existed between the directory-creating
 * {@code @Observes StartupEvent} observer and the {@code @Startup} schedulers'
 * own {@code @PostConstruct}, whose relative ordering was undefined.
 * </p>
 */
@ApplicationScoped
public final class DirectoryProvisioner {
    /**
     * The configured path to the blogs' directory.
     */
    private final String blogsDirectoryPath;

    /**
     * The configured path to the images' directory.
     */
    private final String imagesDirectoryPath;

    /**
     * The resolved blogs directory, created in {@link #provision()}.
     */
    private Path blogsDirectory;

    /**
     * The resolved images directory, created in {@link #provision()}.
     */
    private Path imagesDirectory;

    /**
     * Creates the provisioner with its configured directory paths.
     *
     * @param blogsDirectoryPath  The path to the blogs' directory.
     * @param imagesDirectoryPath The path to the images' directory.
     */
    @Inject
    DirectoryProvisioner(
            @ConfigProperty(
                    name = "application.blogs-directory",
                    defaultValue = "blogs"
            ) final String blogsDirectoryPath,
            @ConfigProperty(
                    name = "application.images-directory",
                    defaultValue = "static/images"
            ) final String imagesDirectoryPath
    ) {
        this.blogsDirectoryPath = blogsDirectoryPath;
        this.imagesDirectoryPath = imagesDirectoryPath;
    }

    /**
     * Creates the blogs and images directories if they do not already exist.
     * Runs once, at bean construction, before any dependent scheduler bean is
     * put into service. A failure to create either directory is unrecoverable:
     * the application is asked to exit and construction is aborted so no
     * scheduler registers a watch on a missing directory.
     */
    @PostConstruct
    void provision() {
        blogsDirectory = configureDirectory(blogsDirectoryPath);
        if (blogsDirectory == null) {
            Log.fatal("The blogs directory could not be created.");
            Quarkus.asyncExit();
            throw new IllegalStateException("Failed to provision the blogs directory.");
        }
        Log.info("Successfully configured the blogs directory reference.");

        imagesDirectory = configureDirectory(imagesDirectoryPath);
        if (imagesDirectory == null) {
            Log.fatal("The images directory could not be created.");
            Quarkus.asyncExit();
            throw new IllegalStateException("Failed to provision the images directory.");
        }
        Log.info("Successfully configured the images directory reference.");
    }

    /**
     * Returns the resolved blogs directory. The directory is guaranteed to
     * exist for any caller, since this bean's {@link #provision()} runs before
     * a dependent bean can invoke this method.
     *
     * @return The blogs directory {@link Path}.
     */
    public Path blogsDirectory() {
        return blogsDirectory;
    }

    /**
     * Returns the resolved images directory. The directory is guaranteed to
     * exist for any caller, since this bean's {@link #provision()} runs before
     * a dependent bean can invoke this method.
     *
     * @return The images directory {@link Path}.
     */
    public Path imagesDirectory() {
        return imagesDirectory;
    }

    /**
     * Creates or retrieves the {@link Path} instance resolved by the provided
     * {@link String} path or null in case of failure.
     *
     * @param path The path to the directory which needs to be configured.
     * @return A {@link Path} instance of the directory which is resolved by
     * the provided path. If null is returned than the method failed to create
     * a directory.
     */
    private static Path configureDirectory(final String path) {
        final File directory = new File(path);

        if (!directory.exists()) {
            Log.warnf("The '%s' directory does not exist. Creating one now.", path);

            if (!directory.mkdirs()) {
                return null;
            }

            Log.infof("The '%s' directory was created.", path);
        }

        return directory.toPath();
    }
}
