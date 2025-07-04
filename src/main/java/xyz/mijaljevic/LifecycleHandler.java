package xyz.mijaljevic;

import io.quarkus.logging.Log;
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
     * The path to the blogs' directory. Configured using the
     * "application.blogs-directory" property.
     */
    @ConfigProperty(
            name = "application.blogs-directory",
            defaultValue = "blogs"
    )
    String blogsDirectoryPath;

    /**
     * The path to the images' directory. Configured using the
     * "application.images-directory" property.
     */
    @ConfigProperty(
            name = "application.images-directory",
            defaultValue = "static/images"
    )
    String imagesDirectoryPath;

    /**
     * The path to the CSS directory. Configured using the
     * "application.css-directory" property.
     */
    @ConfigProperty(
            name = "application.css-directory",
            defaultValue = "static/css"
    )
    String cssDirectoryPath;

    /**
     * The path to the RSS feed file. Configured using the
     * "application.rss-feed" property.
     */
    @ConfigProperty(
            name = "application.rss-feed",
            defaultValue = "rss.xml"
    )
    String rssFilePath;

    /**
     * This method is called when the application starts. It configures the
     * directories and initializes the RSS feed.
     *
     * @param event The startup event.
     */
    void onStart(@Observes StartupEvent event) {
        Website.BlogsDirectory = configureDirectory(blogsDirectoryPath);
        if (Website.BlogsDirectory == null) {
            ExitCodes.BLOGS_DIRECTORY_SETUP_FAILED.logAndExit();
        }
        Log.info("Successfully configured the blogs directory reference.");

        Website.ImagesDirectory = configureDirectory(imagesDirectoryPath);
        if (Website.ImagesDirectory == null) {
            ExitCodes.IMAGES_DIRECTORY_SETUP_FAILED.logAndExit();
        }
        Log.info("Successfully configured the images directory reference.");

        Website.CssDirectory = configureDirectory(cssDirectoryPath);
        if (Website.CssDirectory == null) {
            ExitCodes.CSS_DIRECTORY_SETUP_FAILED.logAndExit();
        }
        Log.info("Successfully configured the css directory reference.");

        if (!RssFeed.initializeRssFeed(rssFilePath)) {
            ExitCodes.RSS_FILE_PARSING_FAILED.logAndExit();
        }
        Log.info("Successfully initialized the RSS feed root content.");
    }

    /**
     * Creates or retrieves the {@link Path} instance resolved by the provided
     * {@link String} path or null in case of failure.
     *
     * @param path The path to the directory which needs to be configured.
     * @return A {@link Path} instance of the directory which is resolved by the
     * provided path. If null is returned than the method failed to create a
     * directory.
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
