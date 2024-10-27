package xyz.mijaljevic;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import xyz.mijaljevic.orm.model.Blog;
import xyz.mijaljevic.orm.model.StaticFile;

/**
 * Application class. Implements the {@link QuarkusApplication} interface.
 * Contains the application caches and "<i>global</i>" variables.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
public final class Website implements QuarkusApplication
{
	/**
	 * This map contains the blogs as an in memory cache. The keys are the blog file
	 * names while the values are blog models. This cache is initialized during
	 * startup and periodically refreshed using the refresh blog cache task.
	 */
	public static final ConcurrentHashMap<String, Blog> BLOG_CACHE = new ConcurrentHashMap<String, Blog>();

	/**
	 * This map contains the static files as an in memory cache. The keys are the
	 * static file names while the values are static models. This cache is
	 * initialized during startup and periodically refreshed using the refresh
	 * static cache tasks(each static file type has its own task).
	 */
	public static final ConcurrentHashMap<String, StaticFile> STATIC_CACHE = new ConcurrentHashMap<String, StaticFile>();

	/**
	 * {@link File} instance which holds the reference to the blogs directory.
	 * Configured during application startup by the {@link LifecycleHandler} class.
	 */
	public static File BlogsDirectory = null;

	/**
	 * {@link File} instance which holds the reference to the images directory.
	 * Configured during application startup by the {@link LifecycleHandler} class.
	 */
	public static File ImagesDirectory = null;

	/**
	 * {@link File} instance which holds the reference to the css directory.
	 * Configured during application startup by the {@link LifecycleHandler} class.
	 */
	public static File CssDirectory = null;

	@Override
	public final int run(String... args) throws Exception
	{
		Quarkus.waitForExit();

		return 0;
	}
}
