package xyz.mijaljevic;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;

/**
 * Contains application exit codes and their descriptions. Also, helper method
 * for <i>Quarkus.AsyncExit()</i> calls when the application needs to shut
 * down.
 */
public enum ExitCodes {
    BLOGS_DIRECTORY_SETUP_FAILED(1, "Failed to create and configure a blogs directory!"),
    IMAGES_DIRECTORY_SETUP_FAILED(2, "Failed to create and configure a images directory!"),
    CSS_DIRECTORY_SETUP_FAILED(3, "Failed to create and configure a CSS directory!"),
    WATCH_BLOGS_TASK_WATCH_SERVICE_FAILED(4, "Failed to initialize a WatchService for the WatchBlogsTask!"),
    WATCH_BLOGS_TASK_WATCH_KEY_FAILED(5, "Failed to initialize a WatchKey for the WatchBlogsTask!"),
    WATCH_IMAGES_TASK_WATCH_SERVICE_FAILED(6, "Failed to initialize a WatchService for the WatchImagesTask!"),
    WATCH_IMAGES_TASK_WATCH_KEY_FAILED(7, "Failed to initialize a WatchKey for the WatchImagesTask!"),
    WATCH_CSS_TASK_WATCH_SERVICE_FAILED(8, "Failed to initialize a WatchService for the WatchCssTask!"),
    WATCH_CSS_TASK_WATCH_KEY_FAILED(9, "Failed to initialize a WatchKey for the WatchCssTask!"),
    RSS_JAXB_CONTEXT_INIT_FAILED(10, "Failed to initialize a JAXBContext for the Rss class!"),
    RSS_FILE_PARSING_FAILED(11, "Failed to parse the RSS feed XML file!"),
    HASH_ALGORITHM_MISSING(12, "Hash algorithm requested by the website not found in the JRE!");

    private final int code;
    private final String description;

    ExitCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Logs the exit code description as an <i><b>FATAL</b></i> error and exits
     * the application asynchronously using <i>Quarkus.AsyncExit()</i>.
     */
    public final void logAndExit() {
        Log.fatal(description);

        Quarkus.asyncExit(code);
    }
}
