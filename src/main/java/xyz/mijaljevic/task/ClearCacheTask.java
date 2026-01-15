/**
 * Copyright (C) 2025 Karlo Mijaljević
 *
 * <p>
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * </p>
 *
 * <p>
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * </p>
 *
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 * </p>
 */
package xyz.mijaljevic.task;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.entity.Blog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * Clears the blogs cache, removes blogs that have not been read for a long
 * time, while keeping the often read blogs in memory. It does this every hour.
 * </p>
 *
 * <p>
 * Note that it will not clear the data of the recent blogs, served by the
 * home page and the RSS feed.
 * </p>
 */
@ApplicationScoped
final class ClearCacheTask {
    /**
     * Defines the maximum amount of hours that a blog can be not read for until
     * it is removed from the blogs cache.
     */
    private static final int MAX_NOT_READ_HOURS = 4;

    /**
     * <p>
     * Clears the blogs cache, removes blogs that have not been read for a long
     * time, while keeping the often read blogs in memory. It does this every hour.
     * </p>
     *
     * <p>
     * Note that it will not clear the data of the recent blogs, served by the
     * home page and the RSS feed.
     * </p>
     */
    @Scheduled(
            identity = "clear_blogs_cache",
            every = "1h",
            delayed = "5s"
    )
    void clearBlogsCache() {
        final List<Blog> recent = Website.retrieveRecentBlogs();

        Website.BLOG_CACHE
                .entrySet()
                .stream()
                .filter(entry -> !recent.contains(entry.getValue())
                        && entry.getValue().getLastRead() != null
                )
                .forEach(entry -> {
                    final LocalDateTime lastRead = entry.getValue().getLastRead();

                    final boolean isBefore = lastRead.isBefore(
                            LocalDateTime.now().minusHours(MAX_NOT_READ_HOURS)
                    );

                    if (isBefore) {
                        entry.getValue().setData(null);

                        Log.info("Cleared data from cache for blog with file name: " + entry.getKey());
                    }
                });
    }
}
