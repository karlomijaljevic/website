package xyz.mijaljevic.task;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.orm.BlogService;
import xyz.mijaljevic.orm.model.Blog;
import xyz.mijaljevic.web.page.PageHelper;

/**
 * Class contains a scheduled method that runs every 5 minutes and checks the
 * {@link WatchKey} that was initialized during class creation. The key monitors
 * the creation/update/deletion of the blogs directory and
 * creates/updates/deletes entries in the DB accordingly.
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0.0
 */
@ApplicationScoped
public final class WatchBlogsTask
{
	@Inject
	private BlogService blogService;

	/**
	 * Holds the reference to the blogs directory {@link WatchKey}.
	 */
	private static WatchKey WatchKey = null;

	/**
	 * True when the {@link WatchKey} is valid and false otherwise.
	 */
	private static boolean WatchKeyValid = false;

	/**
	 * {@link Pattern} instance used to match blog titles.
	 */
	private static final Pattern BLOG_TITLE_PATTERN = Pattern.compile("<h1 class=\\\"page\\-title\\\">(.*)</h1>");

	/**
	 * Initializes the class {@link WatchKey} variable <i>WatchKey</i> and performs
	 * the initial blogs directory check up for new or updated files. It also
	 * compares the database blogs against the files to check which DB blog has lost
	 * its file if any and then removes the entity from the DB.
	 */
	@PostConstruct
	void initWatchBlogsTask()
	{
		WatchService watcher = null;

		try
		{
			watcher = FileSystems.getDefault().newWatchService();
		}
		catch (IOException e)
		{
			ExitCodes.WATCH_BLOGS_TASK_WATCH_SERVICE_FAILED.logAndExit();
		}

		Path directory = Website.BlogsDirectory.toPath();

		try
		{
			WatchKey = directory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

			WatchKeyValid = WatchKey.isValid();
		}
		catch (IOException e)
		{
			ExitCodes.WATCH_BLOGS_TASK_WATCH_KEY_FAILED.logAndExit();
		}

		File[] files = Website.BlogsDirectory.listFiles();
		List<String> fileNames = new ArrayList<String>();

		for (File file : files)
		{
			consumeBlogFile(file);
			fileNames.add(file.getName());
		}

		List<Blog> blogs = blogService.listAllBlogsMissingFromFileNames(fileNames);

		for (Blog blog : blogs)
		{
			Log.info("Found blog entity without file. Deleting it. File: " + blog.getFileName());

			blogService.deleteBlog(blog);
		}

		PageHelper.updateCacheControlHeaders();
	}

	/**
	 * Runs every 5 minutes and checks the {@link WatchKey} that was initialized
	 * during class creation. The key monitors the creation/update/deletion of the
	 * blogs directory and this method creates/updates/deletes entries in the DB
	 * accordingly.
	 */
	@Scheduled(identity = "watch_blogs_task", every = "5m")
	final void runWatchBlogsTask()
	{
		if (!WatchKeyValid)
		{
			Log.fatal("WatchKey for the WatchBlogsTask is not valid. Exiting watch_blogs_directory task!");

			return;
		}

		boolean changeOccurred = false;

		for (WatchEvent<?> event : WatchKey.pollEvents())
		{
			WatchEvent.Kind<?> kind = event.kind();

			if (kind == StandardWatchEventKinds.OVERFLOW)
			{
				Log.error("OVERFLOW event occured while watching the blogs directory!");

				continue;
			}

			@SuppressWarnings("unchecked")
			WatchEvent<Path> ev = (WatchEvent<Path>) event;

			Path filename = ev.context();

			File file = Website.BlogsDirectory.toPath().resolve(filename).toFile();

			if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY)
			{
				if (!consumeBlogFile(file))
				{
					continue;
				}
			}
			else
			{
				Blog blog = blogService.findBlogByFileName(file.getName());

				if (blogService.deleteBlog(blog))
				{
					Website.BLOG_CACHE.remove(blog.getFileName());
					Log.info("Successfully deleted blog of file: " + file.getName());
				}
			}

			changeOccurred = true;
		}

		if (changeOccurred)
		{
			PageHelper.updateCacheControlHeaders();
		}

		WatchKeyValid = WatchKey.reset();
	}

	/**
	 * Consumes the provided {@link Blog} {@link File} and either updates or creates
	 * a blog entity depending on the state of the blog file against the entity in
	 * the DB.
	 * 
	 * @param file A blog file to consume.
	 * 
	 * @return False in case it failed to parse the file and true if the process was
	 *         successful.
	 */
	private final boolean consumeBlogFile(File file)
	{
		String title = getTitleFromFile(file);

		if (title == null)
		{
			Log.error("Failed to find file " + file.getName() + " title!");
			return false;
		}

		boolean isNew = false;
		Blog blog = blogService.findBlogByFileName(file.getName());

		if (blog == null)
		{
			blog = new Blog();
			isNew = true;
		}

		blog.setTitle(title);
		String oldHash = blog.getHash();

		try
		{
			String hash = TaskHelper.hashFile(file);
			blog.setHash(hash);
		}
		catch (NoSuchAlgorithmException | IOException e)
		{
			Log.error("Failed to hash file " + blog.getFileName() + " with algorithm " + TaskHelper.FILE_HASH_ALGO);
			return false;
		}

		if (isNew)
		{
			blog.setFileName(file.getName());
			blogService.createBlog(blog);
			blog = blogService.findBlogByFileName(file.getName());
			Log.info("Successfully created blog for file: " + file.getName());
		}
		else
		{
			// In case no actual file changes have occurred.
			if (!blog.getHash().equals(oldHash))
			{
				blog = blogService.updateBlog(blog);
				Log.info("Successfully updated blog for file: " + file.getName());
			}
		}

		Website.BLOG_CACHE.put(blog.getFileName(), blog);

		return true;
	}

	/**
	 * Retrieves the blog title from the blog file using regular expressions.
	 * 
	 * @param blog A blog {@link File}
	 * 
	 * @return Either the blog title or null in case of failure.
	 */
	private static final String getTitleFromFile(File blog)
	{
		String title = null;

		try
		{
			title = Files.readAllLines(blog.toPath()).get(0);

			Matcher match = BLOG_TITLE_PATTERN.matcher(title);

			if (match.find())
			{
				title = match.group(1);
			}
		}
		catch (IOException | IndexOutOfBoundsException e)
		{
			Log.error("Failed to get title of file " + blog.getName());
		}

		return title;
	}
}
