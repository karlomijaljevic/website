package xyz.mijaljevic;

import java.io.File;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import xyz.mijaljevic.orm.model.Rss;
import xyz.mijaljevic.web.RssFeed;

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

	@ConfigProperty(name = "application.rss-feed", defaultValue = "rss.xml")
	private String rssFilePath;

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

		Rss rss = readRssFeed(new File(rssFilePath));
		if (rss == null)
		{
			ExitCodes.RSS_FILE_PARSING_FAILED.logAndExit();
		}
		rss.getChannel().setLastBuildDate(RssFeed.DEFAULT_LAST_BUILD_DATE);
		RssFeed.updateRssFeed(rss);
		Log.info("Successfully initialized the RSS feed root content.");
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

	/**
	 * Parses the provided file into a {@link Rss} instance. In case the provided
	 * file was not a RSS XML the method returns null.
	 * 
	 * @param file A {@link File} to parse
	 * 
	 * @return Returns a {@link Rss} instance. In case the provided file was not a
	 *         RSS XML the method returns null.
	 */
	private static final Rss readRssFeed(File file)
	{
		try
		{
			Unmarshaller jaxbUnmarshaller = RssFeed.RSS_JAXB_CONTEXT.createUnmarshaller();
			return (Rss) jaxbUnmarshaller.unmarshal(file);
		}
		catch (JAXBException e)
		{
			Log.error("Failed to parse the RSS feed XML file!", e);

			return null;
		}
	}
}
