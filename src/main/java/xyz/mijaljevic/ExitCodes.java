package xyz.mijaljevic;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;

/**
 * Contains application exit codes and their descriptions. Also helper method
 * for <i>Quarkus.AsyncExit()</i> calls when the application needs to shut down.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
public enum ExitCodes
{
	BLOGS_DIRECTORY_SETUP_FAILED(1, "Failed to create and configure a blogs directory!"),
	IMAGES_DIRECTORY_SETUP_FAILED(2, "Failed to create and configure a images directory!"),
	CSS_DIRECTORY_SETUP_FAILED(3, "Failed to create and configure a CSS directory!"),
	WATCH_BLOGS_TASK_WATCH_SERVICE_FAILED(4, "Failed to initialize a WatchService for the WatchBlogsTask!"),
	WATCH_BLOGS_TASK_WATCH_KEY_FAILED(5, "Failed to initialize a WatchKey for the WatchBlogsTask!"),
	WATCH_IMAGES_TASK_WATCH_SERVICE_FAILED(6, "Failed to initialize a WatchService for the WatchImagesTask!"),
	WATCH_IMAGES_TASK_WATCH_KEY_FAILED(7, "Failed to initialize a WatchKey for the WatchImagesTask!"),
	WATCH_CSS_TASK_WATCH_SERVICE_FAILED(8, "Failed to initialize a WatchService for the WatchCssTask!"),
	WATCH_CSS_TASK_WATCH_KEY_FAILED(9, "Failed to initialize a WatchKey for the WatchCssTask!");

	private int code;
	private String description;

	private ExitCodes(int code, String description)
	{
		this.code = code;
		this.description = description;
	}

	/**
	 * Logs the exit code description as an <i><b>FATAL</b></i> error and exits the
	 * application asynchronously using <i>Quarkus.AsyncExit()</i>.
	 */
	public final void logAndExit()
	{
		Log.fatal(description);

		Quarkus.asyncExit(code);
	}

	public int getCode()
	{
		return code;
	}

	public String getDescription()
	{
		return description;
	}
}