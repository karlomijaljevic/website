package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import xyz.mijaljevic.model.entity.Blog;
import xyz.mijaljevic.model.entity.StaticFile;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application class. Implements the {@link QuarkusApplication} interface.
 * Contains the application caches and "<i>global</i>" variables.
 */
public final class Website implements QuarkusApplication {
    /**
     * This map contains the blogs as an in memory cache. The keys are the blog file
     * names while the values are blog models. This cache is initialized during
     * startup and periodically refreshed using the refresh blog cache task.
     */
    public static final ConcurrentHashMap<String, Blog> BLOG_CACHE = new ConcurrentHashMap<>();

    /**
     * This map contains the static files as an in memory cache. The keys are the
     * static file names while the values are static models. This cache is
     * initialized during startup and periodically refreshed using the refresh
     * static cache tasks(each static file type has its own task).
     */
    public static final ConcurrentHashMap<String, StaticFile> STATIC_CACHE = new ConcurrentHashMap<>();

    /**
     * Limit of latest blogs to display on the Home page and RSS feed.
     */
    public static final int NUMBER_OF_BLOGS_TO_DISPLAY = 8;

    /**
     * Time zone used by the website for clients.
     */
    public static final ZoneId TIME_ZONE = ZoneId.of("UTC");

    /**
     * Hash algorithm used by the website for file and Etag hashing.
     */
    public static final String HASH_ALGORITHM = "SHA-256";

    /**
     * {@link Path} instance which holds the reference to the blog directory.
     * Configured during application startup by the {@link LifecycleHandler} class.
     */
    static Path BlogsDirectory = null;

    /**
     * {@link Path} instance which holds the reference to the image directory.
     * Configured during application startup by the {@link LifecycleHandler} class.
     */
    static Path ImagesDirectory = null;

    /**
     * {@link Path} instance which holds the reference to the css directory.
     * Configured during application startup by the {@link LifecycleHandler}
     * class.
     */
    static Path CssDirectory = null;

    @Override
    public int run(String... args) {
        Quarkus.waitForExit();

        return 0;
    }

    /**
     * @return Most recent blogs (ordered by creation date from newest to
     * oldest) from the blogs cache.
     */
    public static List<Blog> retrieveRecentBlogs() {
        return BLOG_CACHE.values()
                .stream()
                .sorted()
                .limit(NUMBER_OF_BLOGS_TO_DISPLAY)
                .toList();
    }

    /**
     * @return The path to the blogs' directory.
     * @throws IllegalStateException if the blogs directory is not set.
     */
    public static Path getBlogsDirectory() {
        if (BlogsDirectory == null) {
            throw new IllegalStateException("Blogs directory is not set. Please check the application configuration.");
        }
        return BlogsDirectory;
    }

    /**
     * @return The path to the images' directory.
     * @throws IllegalStateException if the images directory is not set.
     */
    public static Path getImagesDirectory() {
        if (ImagesDirectory == null) {
            throw new IllegalStateException("Images directory is not set. Please check the application configuration.");
        }
        return ImagesDirectory;
    }

    /**
     * @return The path to the css directory.
     * @throws IllegalStateException if the css directory is not set.
     */
    public static Path getCssDirectory() {
        if (CssDirectory == null) {
            throw new IllegalStateException("CSS directory is not set. Please check the application configuration.");
        }
        return CssDirectory;
    }
}
