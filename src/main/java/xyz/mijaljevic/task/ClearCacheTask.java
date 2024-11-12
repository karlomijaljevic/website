package xyz.mijaljevic.task;

import java.time.LocalDateTime;
import java.util.List;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.entity.Blog;

/**
 * <p>
 * Clears the blogs cache, removes blogs that have not been read for a long
 * time, while keeping the often read blogs in memory. It does this every hour.
 * </p>
 * <p>
 * Due note that it will not clear the data of the recent blogs, served by the
 * home page and the RSS feed.
 * </p>
 * 
 * @author karlo
 * 
 * @since 10.2024
 * 
 * @version 1.0
 */
@ApplicationScoped
final class ClearCacheTask
{
	/**
	 * Defines the maximum amount of hours that a blog can be not read for until it
	 * is removed from the blogs cache.
	 */
	private static final int MAX_NOT_READ_HOURS = 4;

	/**
	 * <p>
	 * Clears the blogs cache, removes blogs that have not been read for a long
	 * time, while keeping the often read blogs in memory. It does this every hour.
	 * </p>
	 * <p>
	 * Due note that it will not clear the data of the recent blogs, served by the
	 * home page and the RSS feed.
	 * </p>
	 */
	@Scheduled(identity = "clear_blogs_cache", every = "1h", delayed = "5s")
	final void clearBlogsCache()
	{
		List<Blog> recent = Website.retrieveRecentBlogs();

		Website.BLOG_CACHE.entrySet().stream().filter(entry -> !recent.contains(entry.getValue())).forEach(entry -> {
			LocalDateTime lastRead = entry.getValue().getLastRead();

			if (lastRead != null && lastRead.isBefore(LocalDateTime.now().minusHours(MAX_NOT_READ_HOURS)))
			{
				entry.getValue().setData(null);

				Log.info("Cleared data from cache for blog with file name: " + entry.getKey());
			}
		});
	}
}
