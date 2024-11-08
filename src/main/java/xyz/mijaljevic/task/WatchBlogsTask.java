package xyz.mijaljevic.task;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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
import xyz.mijaljevic.orm.model.Item;
import xyz.mijaljevic.orm.model.Rss;
import xyz.mijaljevic.web.RssFeed;
import xyz.mijaljevic.web.page.HomePage;
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
	 * Blogs are only HTML files therefore the .html extension is expected. This
	 * {@link Pattern} checks that.
	 */
	private static final Pattern BLOG_FILE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.html$");

	/**
	 * URL used by the website, should stay mijaljevic.xyz as long as I live I hope.
	 */
	private static final String WEBSITE_URL = "https://mijaljevic.xyz/";

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
			if (isFileHtml(file))
			{
				consumeBlogFile(file);
				fileNames.add(file.getName());
			}
		}

		List<Blog> blogs = blogService.listAllBlogsMissingFromFileNames(fileNames);

		for (Blog blog : blogs)
		{
			Log.info("Found blog entity without file. Deleting it. File: " + blog.getFileName());

			blogService.deleteBlog(blog);
		}

		PageHelper.updateCacheControlHeaders();

		updateRssFeed();
	}

	/**
	 * Runs every 5 minutes and checks the {@link WatchKey} that was initialized
	 * during class creation. The key monitors the creation/update/deletion of the
	 * blogs directory and this method creates/updates/deletes entries in the DB
	 * accordingly.
	 */
	@Scheduled(identity = "watch_blogs_task", every = "5m", delayed = "5s")
	final void runWatchBlogsTask()
	{
		if (!WatchKeyValid)
		{
			Log.fatal("WatchKey for the WatchBlogsTask is not valid. Exiting watch_blogs_directory task!");

			return;
		}

		boolean changeOccurred = false;
		boolean blogCreated = false;

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

			if (!isFileHtml(file))
			{
				continue;
			}

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
			// For the RSS FEED update
			blogCreated = kind == StandardWatchEventKinds.ENTRY_CREATE;
		}

		if (changeOccurred)
		{
			PageHelper.updateCacheControlHeaders();
		}

		if (blogCreated)
		{
			updateRssFeed();
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
	 * Updates the items served by the RSS feed.
	 */
	private static final void updateRssFeed()
	{
		List<Item> items = new ArrayList<Item>();

		Website.BLOG_CACHE.values().stream().sorted().limit(HomePage.NUMBER_OF_BLOGS_TO_DISPLAY).forEach(blog -> {
			Item item = new Item();

			String filePath = Website.BlogsDirectory.getPath() + File.separator + blog.getFileName();

			try
			{
				item.setDescription(Files.readString(Paths.get(filePath), StandardCharsets.UTF_8));
			}
			catch (IOException e)
			{
				Log.error("Failed to read the '" + blog.getFileName() + "' blog contents for RSS!", e);

				return;
			}

			item.setGuid(WEBSITE_URL + "blog/" + blog.getId());
			item.setLink(WEBSITE_URL + "blog/" + blog.getId());
			item.setTitle(blog.getTitle());

			String pubDate = ZonedDateTime.from(blog.getCreated().atZone(ZoneId.systemDefault()))
					.format(RssFeed.RSS_SPEC_FORMAT);
			item.setPubDate(pubDate);

			items.add(item);
		});

		Rss current = RssFeed.getRssFeed();

		Blog lastUpdatedBlog = Website.BLOG_CACHE.values().stream().sorted().findFirst().orElse(null);

		if (lastUpdatedBlog != null)
		{
			String pubDate = ZonedDateTime.from(lastUpdatedBlog.getCreated().atZone(ZoneId.systemDefault()))
					.format(RssFeed.RSS_SPEC_FORMAT);
			current.getChannel().setLastBuildDate(pubDate);
		}

		current.getChannel().setItems(items);

		RssFeed.updateRssFeed(current);
	}

	/**
	 * Checks if the provided {@link File} is a HTML file (checks only the file
	 * extension).
	 * 
	 * @param file A {@link File} instance to check.
	 * 
	 * @return Returns true if the file is in the proper format, denoted by the
	 *         <i>BLOG_FILE_PATTERN</i> {@link Pattern} instance. False otherwise.
	 */
	private static final boolean isFileHtml(File file)
	{
		String name = file.getName();

		Matcher match = BLOG_FILE_PATTERN.matcher(name);

		return match.find();
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
