package xyz.mijaljevic;

import java.io.File;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

/**
 * Handles lifecycle events e.g. application startup.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@ApplicationScoped
public final class LifecycleHandler
{
	@ConfigProperty(name = "application.blogs-directory", defaultValue = "blogs")
	private String blogsDirectoryPath;

	@ConfigProperty(name = "application.images-directory", defaultValue = "static/images")
	private String imagesDirectoryPath;

	@ConfigProperty(name = "application.css-directory", defaultValue = "static/css")
	private String cssDirectoryPath;

	final void onStart(@Observes StartupEvent event)
	{
		Website.BlogsDirectory = configureDirectory(blogsDirectoryPath);
		if (Website.BlogsDirectory == null)
		{
			ExitCodes.BLOGS_DIRECTORY_SETUP_FAILED.logAndExit();
		}
		Log.info("Successfully configured the blogs directory reference.");

		Website.ImagesDirectory = configureDirectory(imagesDirectoryPath);
		if (Website.ImagesDirectory == null)
		{
			ExitCodes.IMAGES_DIRECTORY_SETUP_FAILED.logAndExit();
		}
		Log.info("Successfully configured the images directory reference.");

		Website.CssDirectory = configureDirectory(cssDirectoryPath);
		if (Website.CssDirectory == null)
		{
			ExitCodes.CSS_DIRECTORY_SETUP_FAILED.logAndExit();
		}
		Log.info("Successfully configured the css directory reference.");
	}

	/**
	 * Creates or retrieves the an {@link File} instance resolved by the provided
	 * {@link String} path or null in case of failure.
	 * 
	 * @param path The path to the directory which needs to be configured.
	 * 
	 * @return A {@link File} instance of the directory which is resolved by the
	 *         provided path. If null is returned than the method failed to create a
	 *         directory.
	 */
	private static final File configureDirectory(String path)
	{
		File directory = new File(path);

		if (!directory.exists())
		{
			Log.warn("The '" + path + "' directory does not exist. Creating one now.");

			if (!directory.mkdirs())
			{
				return null;
			}

			Log.info("The '" + path + "' directory was created.");
		}

		return directory;
	}
}
