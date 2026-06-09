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
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.web.RssFeed;

import java.io.File;
import java.nio.file.Path;

/**
 * Handles lifecycle events e.g. application startup.
 */
@ApplicationScoped
final class LifecycleHandler {
    /**
     * The path to the blogs' directory.
     */
    private final String blogsDirectoryPath;

    /**
     * The path to the images' directory.
     */
    private final String imagesDirectoryPath;

    /**
     * The path to the RSS feed file. Configured using the
     * "application.rss-feed" property.
     */
    private final String rssFilePath;

    /**
     * Creates the handler with its configured directory and RSS feed paths.
     *
     * @param blogsDirectoryPath  The path to the blogs' directory.
     * @param imagesDirectoryPath The path to the images' directory.
     * @param rssFilePath         The path to the RSS feed file.
     */
    @Inject
    LifecycleHandler(
            @ConfigProperty(
                    name = "application.blogs-directory",
                    defaultValue = "blogs"
            ) final String blogsDirectoryPath,
            @ConfigProperty(
                    name = "application.images-directory",
                    defaultValue = "static/images"
            ) final String imagesDirectoryPath,
            @ConfigProperty(
                    name = "application.rss-feed",
                    defaultValue = "static/rss.xml"
            ) final String rssFilePath
    ) {
        this.blogsDirectoryPath = blogsDirectoryPath;
        this.imagesDirectoryPath = imagesDirectoryPath;
        this.rssFilePath = rssFilePath;
    }

    /**
     * This method is called when the application starts. It configures the
     * directories and initializes the RSS feed.
     *
     * @param event The startup event.
     */
    void onStart(@Observes final StartupEvent event) {
        Path path = configureDirectory(blogsDirectoryPath);
        if (path == null) {
            Log.fatal("The blogs directory could not be created.");
            Quarkus.asyncExit();
        }
        Log.info("Successfully configured the blogs directory reference.");

        path = configureDirectory(imagesDirectoryPath);
        if (path == null) {
            Log.fatal("The images directory could not be created.");
            Quarkus.asyncExit();
        }
        Log.info("Successfully configured the images directory reference.");

        if (!RssFeed.initializeRssFeed(rssFilePath)) {
            Log.fatal("The RSS feed root content could not be initialized.");
            Quarkus.asyncExit();
        }
        Log.info("Successfully initialized the RSS feed root content.");
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
