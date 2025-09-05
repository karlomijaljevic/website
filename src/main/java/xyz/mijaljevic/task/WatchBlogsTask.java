package xyz.mijaljevic.task;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.BlogService;
import xyz.mijaljevic.model.entity.Blog;
import xyz.mijaljevic.utils.MarkdownParser;
import xyz.mijaljevic.utils.TaskUtils;
import xyz.mijaljevic.web.RssFeed;
import xyz.mijaljevic.web.WebPage;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
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
    @Inject
    BlogService blogService;

    /**
     * The path to the blogs' directory.
     */
    @ConfigProperty(
            name = "application.blogs-directory",
            defaultValue = "blogs"
    )
    String blogsDirectoryPath;

    /**
     * Holds the reference to the blogs directory {@link WatchKey}.
     */
    private static WatchKey WatchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean WatchKeyValid = false;

    /**
     * Initializes the class {@link WatchKey} variable <i>WatchKey</i> and
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
            Log.fatal("Watch blogs task failed to initialize WatchService!");
            Quarkus.asyncExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher not available!");
        }

        Path blogsPath = Paths.get(blogsDirectoryPath);

        try {
            WatchKey = blogsPath.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            WatchKeyValid = WatchKey.isValid();
        } catch (IOException e) {
            Log.fatal("Watch blogs task failed! Does the directory exist?");
            Quarkus.asyncExit();
        }

        File[] files = blogsPath.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeBlogFile(file);
            fileNames.add(file.getName());
        }

        List<Blog> blogs = blogService.listAllBlogsMissingFromFileNames(fileNames);

        for (Blog blog : blogs) {
            Log.warn("Found blog without file. Deleting file: " + blog.getFileName());

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
        if (!WatchKeyValid) {
            Log.fatal("BLOG - WatchKey NOT valid! Stopping task!");

            return;
        }

        boolean changeOccurred = false;
        boolean blogCreated = false;

        for (WatchEvent<?> event : WatchKey.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("BLOG - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;

            Path filename = ev.context();

            Path blogsPath = Paths.get(blogsDirectoryPath);

            File file = blogsPath.resolve(filename).toFile();

            if (!isMarkdownFile(file)) {
                Log.warn("BLOG - Not a markdown file: " + file.getName());
                continue;
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (!consumeBlogFile(file)) {
                    continue;
                }
            } else {
                Blog blog = blogService.findBlogByFileName(file.getName());

                if (blog != null && blogService.deleteBlog(blog)) {
                    Website.BLOG_CACHE.remove(blog.getFileName());
                    Log.info("Successfully deleted blog of file: " + file.getName());
                }
            }

            changeOccurred = true;

            // For the RSS FEED update
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

        WatchKeyValid = WatchKey.reset();
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
    private boolean consumeBlogFile(File file) {
        String title = MarkdownParser.getTitleFromFile(file);

        boolean isNew = false;

        Blog blog = blogService.findBlogByFileName(file.getName());

        if (blog == null) {
            blog = new Blog();
            isNew = true;
        }

        blog.setTitle(title);
        String oldHash = blog.getHash();

        try {
            String hash = TaskUtils.hashFile(file);
            blog.setHash(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.error("Failed to hash file " + blog.getFileName() + " with algorithm " + Website.HASH_ALGORITHM);
            return false;
        }

        if (isNew) {
            blog.setFileName(file.getName());
            blogService.createBlog(blog);
            blog = blogService.findBlogByFileName(file.getName());
            Log.info("Successfully created blog for file: " + file.getName());
        } else {
            // In case no actual file changes have occurred.
            if (!blog.getHash().equals(oldHash)) {
                blog = blogService.updateBlog(blog);
                Log.info("Successfully updated blog for file: " + file.getName());
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
    private static boolean isMarkdownFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        String fileName = file.getName().toLowerCase();

        return fileName.endsWith(".md");
    }
}
