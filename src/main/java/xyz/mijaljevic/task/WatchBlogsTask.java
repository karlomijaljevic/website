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
import io.quarkus.runtime.Quarkus;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.domain.service.BlogService;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.utils.MarkdownParser;
import xyz.mijaljevic.utils.TaskUtils;
import xyz.mijaljevic.web.RssFeed;
import xyz.mijaljevic.web.WebPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains a scheduled method that runs every 5 minutes and checks the
 * {@link WatchKey} that was initialized during class creation. The key
 * monitors the creation/update/deletion of the blogs directory and
 * creates/updates/deletes entries in the DB accordingly.
 *
 * <p>
 * During initialization fills the RSS feed with the initial set of blogs and
 * when a new blog is added it adds them to the RSS feed accordingly. It does
 * both of these operations calling the {@link RssFeed} <i>updateRssFeed()</i>
 * method.
 * </p>
 */
@ApplicationScoped
final class WatchBlogsTask {
    /**
     * The service that handles the {@link Blog} entity CRUD operations.
     */
    private final BlogService blogService;

    /**
     * The path to the blogs' directory.
     */
    private final String blogsDirectoryPath;

    @Inject
    WatchBlogsTask(
            final BlogService blogService,
            @ConfigProperty(
                    name = "application.blogs-directory",
                    defaultValue = "blogs"
            ) final String blogsDirectoryPath
    ) {
        this.blogService = blogService;
        this.blogsDirectoryPath = blogsDirectoryPath;
    }

    /**
     * Holds the reference to the blogs directory {@link WatchKey}.
     */
    private static WatchKey watchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean watchKeyValid = false;

    /**
     * Initializes the class {@link WatchKey} variable <i>watchKey</i> and
     * performs the initial blogs directory check up for new or updated files.
     *
     * <p>
     * Furthermore, it also compares the database blogs against the files to
     * check which DB blog has lost its file if any and then removes the entity
     * from the DB.
     * </p>
     *
     * <p>
     * Lastly it also initializes the RSS feed items aka the home page blogs.
     * </p>
     */
    @PostConstruct
    void initWatchBlogsTask() {
        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Log.fatal("Watch blogs task failed to initialize WatchService!", e);
            Quarkus.asyncExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher not available!");
        }

        final Path blogsPath = Paths.get(blogsDirectoryPath);

        try {
            watchKey = blogsPath.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            watchKeyValid = watchKey.isValid();
        } catch (IOException e) {
            Log.fatalf(e, "Watch blogs task failed! Does the directory '%s' exist?", blogsDirectoryPath);
            Quarkus.asyncExit();
        }

        final File[] files = blogsPath.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        final List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeBlogFile(file);
            fileNames.add(file.getName());
        }

        final List<Blog> blogs = blogService.listAllBlogsMissingFromFileNames(fileNames);

        for (Blog blog : blogs) {
            Log.warnf("Found blog without file. Deleting file: %s", blog.getFileName());

            blogService.deleteBlog(blog);
        }

        WebPage.updateCacheControlHeaders();

        RssFeed.updateRssFeed(blogsDirectoryPath);
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was
     * initialized during class creation. The key monitors the blogs directory
     * and this method creates/updates/deletes entries in the DB accordingly.
     */
    @Scheduled(
            identity = "watch_blogs_task",
            every = "5m",
            delayed = "5s"
    )
    void runWatchBlogsTask() {
        if (!watchKeyValid) {
            Log.fatal("BLOG - WatchKey NOT valid! Stopping task!");

            return;
        }

        boolean changeOccurred = false;
        boolean blogCreated = false;

        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("BLOG - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked")
            final WatchEvent<Path> ev = (WatchEvent<Path>) event;

            final Path filename = ev.context();

            final Path blogsPath = Paths.get(blogsDirectoryPath);

            final File file = blogsPath.resolve(filename).toFile();

            if (!isMarkdownFile(file)) {
                Log.warnf("BLOG - Not a markdown file: %s", file.getName());
                continue;
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (!consumeBlogFile(file)) {
                    continue;
                }
            } else {
                final Blog blog = blogService.findBlogByFileName(file.getName());

                if (blog != null && blogService.deleteBlog(blog)) {
                    Website.BLOG_CACHE.remove(blog.getFileName());
                    Log.infof("Successfully deleted blog of file: %s", file.getName());
                }
            }

            changeOccurred = true;

            // NOTE: Track new-blog creation to trigger the RSS feed update.
            if (!blogCreated) {
                blogCreated = kind == StandardWatchEventKinds.ENTRY_CREATE;
            }
        }

        if (changeOccurred) {
            WebPage.updateCacheControlHeaders();
        }

        if (blogCreated) {
            RssFeed.updateRssFeed(blogsDirectoryPath);
        }

        watchKeyValid = watchKey.reset();
    }

    /**
     * Consumes the provided {@link Blog} {@link File} and either updates or
     * creates a blog entity depending on the state of the blog file against
     * the entity in the DB.
     *
     * @param file A blog file to consume.
     * @return False in case it failed to parse the file and true if the
     * process was successful.
     */
    private boolean consumeBlogFile(final File file) {
        final String title = MarkdownParser.getTitleFromFile(file);

        boolean isNew = false;

        Blog blog = blogService.findBlogByFileName(file.getName());

        if (blog == null) {
            blog = new Blog();
            isNew = true;
        }

        blog.setTitle(title);
        final String oldHash = blog.getHash();

        try {
            final String hash = TaskUtils.hashFile(file);
            blog.setHash(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.errorf(e, "Failed to hash file %s with algorithm %s", blog.getFileName(), Website.HASH_ALGORITHM);
            return false;
        }

        if (isNew) {
            blog.setFileName(file.getName());
            blogService.createBlog(blog);
            blog = blogService.findBlogByFileName(file.getName());
            Log.infof("Successfully created blog for file: %s", file.getName());
        } else {
            // NOTE: Skip persisting when no actual file changes have occurred.
            if (!blog.getHash().equals(oldHash)) {
                blog = blogService.updateBlog(blog);
                Log.infof("Successfully updated blog for file: %s", file.getName());
            }
        }

        if (blog != null) Website.BLOG_CACHE.put(blog.getFileName(), blog);

        return true;
    }

    /**
     * Checks if the provided file is a markdown file by checking its
     * extension.
     *
     * @param file The file to check.
     * @return True if the file is a markdown file and false otherwise.
     */
    private static boolean isMarkdownFile(final File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        final String fileName = file.getName().toLowerCase();

        return fileName.endsWith(".md");
    }
}
