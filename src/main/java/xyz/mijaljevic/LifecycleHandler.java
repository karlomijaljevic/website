package xyz.mijaljevic;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
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
    @ConfigProperty(
            name = "application.blogs-directory",
            defaultValue = "blogs"
    )
    String blogsDirectoryPath;

    /**
     * The path to the images' directory.
     */
    @ConfigProperty(
            name = "application.images-directory",
            defaultValue = "static/images"
    )
    String imagesDirectoryPath;

    /**
     * The path to the RSS feed file. Configured using the
     * "application.rss-feed" property.
     */
    @ConfigProperty(
            name = "application.rss-feed",
            defaultValue = "static/rss.xml"
    )
    String rssFilePath;

    /**
     * This method is called when the application starts. It configures the
     * directories and initializes the RSS feed.
     *
     * @param event The startup event.
     */
    void onStart(@Observes StartupEvent event) {
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
    private static Path configureDirectory(String path) {
        File directory = new File(path);

        if (!directory.exists()) {
            Log.warn("The '" + path + "' directory does not exist. Creating one now.");

            if (!directory.mkdirs()) {
                return null;
            }

            Log.info("The '" + path + "' directory was created.");
        }

        return directory.toPath();
    }
}
