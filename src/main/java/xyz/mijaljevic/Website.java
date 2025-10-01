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
package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.domain.entity.StaticFile;

import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application class. Implements the {@link QuarkusApplication} interface.
 * Contains the application caches and "<i>global</i>" variables.
 */
public final class Website implements QuarkusApplication {
    /**
     * This map contains the blogs as an in memory cache. The keys are the blog
     * file names while the values are blog models. This cache is initialized
     * during startup and periodically refreshed using the refresh blog cache
     * task.
     */
    public static final ConcurrentHashMap<String, Blog> BLOG_CACHE = new ConcurrentHashMap<>();

    /**
     * This map contains the static files as an in memory cache. The keys are
     * the static file names while the values are static models. This cache is
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
}
