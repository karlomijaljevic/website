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
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.BlogService;
import xyz.mijaljevic.model.BlogTopicService;
import xyz.mijaljevic.model.TopicService;
import xyz.mijaljevic.model.entity.Blog;
import xyz.mijaljevic.model.entity.BlogTopic;
import xyz.mijaljevic.model.entity.Topic;
import xyz.mijaljevic.web.RssFeed;
import xyz.mijaljevic.web.WebHelper;

/**
 * <p>
 * Contains a scheduled method that runs every 5 minutes and checks the
 * {@link WatchKey} that was initialized during class creation. The key monitors
 * the creation/update/deletion of the blogs directory and
 * creates/updates/deletes entries in the DB accordingly.
 * </p>
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

    @Inject
    TopicService topicService;

    @Inject
    BlogTopicService blogTopicService;

    /**
     * Holds the reference to the blogs directory {@link WatchKey}.
     */
    private static WatchKey WatchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean WatchKeyValid = false;

    /**
     * {@link Pattern} instance used to match HTML line values.
     */
    private static final Pattern HTML_LINE_VALUE_PATTERN = Pattern.compile(">(.*)<");

    /**
     * Blogs are only HTML files therefore the .html extension is expected. This
     * {@link Pattern} checks that.
     */
    private static final Pattern BLOG_FILE_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]+\\.html$");

    /**
     * {@link String} instance used to match blog topics block start.
     */
    private static final String BLOG_TOPIC_BLOCK_START = "<div class=\"topics\">";

    /**
     * {@link String} instance used to match blog topics block end.
     */
    private static final String BLOG_TOPIC_BLOCK_END = "</div>";

    /**
     * {@link String} instance used to match blog topics dividers.
     */
    private static final String BLOG_TOPIC_DIVIDER = "<span class=\"topic-divider\">";

    /**
     * <p>
     * Initializes the class {@link WatchKey} variable <i>WatchKey</i> and performs
     * the initial blogs directory check up for new or updated files. It also
     * compares the database blogs against the files to check which DB blog has lost
     * its file if any and then removes the entity from the DB.
     * </p>
     * <p>
     * Furthermore it also initializes the RSS feed items aka the home page blogs.
     * </p>
     */
    @PostConstruct
    void initWatchBlogsTask() {
        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            ExitCodes.WATCH_BLOGS_TASK_WATCH_SERVICE_FAILED.logAndExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher not available!");
        }

        try {
            WatchKey = Website.BlogsDirectory.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKeyValid = WatchKey.isValid();
        } catch (IOException e) {
            ExitCodes.WATCH_BLOGS_TASK_WATCH_KEY_FAILED.logAndExit();
        }

        File[] files = Website.BlogsDirectory.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            if (isFileHtml(file)) {
                consumeBlogFile(file);
                fileNames.add(file.getName());
            }
        }

        List<Blog> blogs = blogService.listAllBlogsMissingFromFileNames(fileNames);

        for (Blog blog : blogs) {
            Log.info("Found blog entity without file. Deleting it. File: " + blog.getFileName());

            blogService.deleteBlog(blog);
        }

        WebHelper.updateCacheControlHeaders();

        RssFeed.updateRssFeed();
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was initialized
     * during class creation. The key monitors the creation/update/deletion of the
     * blogs directory and this method creates/updates/deletes entries in the DB
     * accordingly.
     */
    @Scheduled(identity = "watch_blogs_task", every = "5m", delayed = "5s")
    void runWatchBlogsTask() {
        if (!WatchKeyValid) {
            Log.fatal("WatchKey for the WatchBlogsTask is not valid. Exiting watch_blogs_directory task!");

            return;
        }

        boolean changeOccurred = false;
        boolean blogCreated = false;

        for (WatchEvent<?> event : WatchKey.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("OVERFLOW event occurred while watching the blogs directory!");

                continue;
            }

            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;

            Path filename = ev.context();

            File file = Website.BlogsDirectory.resolve(filename).toFile();

            if (!isFileHtml(file)) {
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
            blogCreated = kind == StandardWatchEventKinds.ENTRY_CREATE;
        }

        if (changeOccurred) {
            WebHelper.updateCacheControlHeaders();
        }

        if (blogCreated) {
            RssFeed.updateRssFeed();
        }

        WatchKeyValid = WatchKey.reset();
    }

    /**
     * Consumes the provided {@link Blog} {@link File} and either updates or creates
     * a blog entity depending on the state of the blog file against the entity in
     * the DB.
     *
     * @param file A blog file to consume.
     * @return False in case it failed to parse the file and true if the process was
     * successful.
     */
    private boolean consumeBlogFile(File file) {
        String title = getTitleFromFile(file);

        if (title == null) {
            Log.error("Failed to find file " + file.getName() + " title!");
            return false;
        }

        boolean isNew = false;
        Blog blog = blogService.findBlogByFileName(file.getName());

        if (blog == null) {
            blog = new Blog();
            isNew = true;
        }

        blog.setTitle(title);
        String oldHash = blog.getHash();

        try {
            String hash = TaskHelper.hashFile(file);
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

        handleTopicsFromBlog(blog, file);

        if (blog != null) {
            Website.BLOG_CACHE.put(blog.getFileName(), blog);
        }

        return true;
    }

    /**
     * It creates blog topics from the provided blog {@link File} and assigns them
     * to the provided {@link Blog} entity or if they already exist it will just
     * assign them.
     *
     * @param blog     A {@link Blog} entity to assign the new topics to
     * @param blogFile A {@link File} instance to check for topics
     */
    private void handleTopicsFromBlog(Blog blog, File blogFile) {
        List<String> lines;

        try {
            lines = Files.readAllLines(blogFile.toPath());
        } catch (IOException e) {
            Log.error("Failed to get topics of file " + blogFile.getName());
            return;
        }

        try {
            if (!lines.get(1).contains(BLOG_TOPIC_BLOCK_START)) {
                Log.warn("No topics defined for file " + blogFile.getName());
                return;
            }
        } catch (IndexOutOfBoundsException e) {
            Log.warn("Only one line in entire blog " + blogFile.getName() + "! Check if it is just an image.");
            return;
        }

        boolean topicStart = false;
        boolean topicEnd = false;
        int row = 0;

        while (!(topicStart && topicEnd)) {
            if (row == lines.size()) {
                break;
            }

            // TODO: Fix issue where all links are treated as topics

            String line = lines.get(row);

            if (line.contains(BLOG_TOPIC_BLOCK_START)) {
                topicStart = true;
            } else if (topicStart && !line.contains(BLOG_TOPIC_DIVIDER)) {
                Matcher match = HTML_LINE_VALUE_PATTERN.matcher(line);

                if (match.find()) {
                    handleTopic(blog, match.group(1).replace("#", ""));
                }
            } else if (topicStart && line.contains(BLOG_TOPIC_BLOCK_END)) {
                topicEnd = true;
            }

            row++;
        }
    }

    /**
     * Called by <i><code>handleTopicsFromBlog(File)</code></i> to assign the topic
     * to the provided blog.
     *
     * @param blog A {@link Blog} entity to assign the new topics to
     * @param name A {@link String} topic name
     */
    private void handleTopic(Blog blog, String name) {
        Topic topic = topicService.findTopicByName(name);

        if (topic == null) {
            topic = new Topic();
            topic.setName(name);
        }

        topicService.createTopic(topic);
        topic = topicService.findTopicByName(name);

        BlogTopic blogTopic = new BlogTopic(blog, topic);

        blogTopicService.createBlogTopic(blogTopic);
    }

    /**
     * Checks if the provided {@link File} is an HTML file (checks only the file
     * extension).
     *
     * @param file A {@link File} instance to check.
     * @return Returns true if the file is in the proper format, denoted by the
     * <i>BLOG_FILE_PATTERN</i> {@link Pattern} instance. False otherwise.
     */
    private static boolean isFileHtml(File file) {
        String name = file.getName();

        Matcher match = BLOG_FILE_PATTERN.matcher(name);

        return match.find();
    }

    /**
     * Retrieves the blog title from the blog file using regular expressions.
     *
     * @param blog A blog {@link File}
     * @return Either the blog title or null in case of failure.
     */
    private static String getTitleFromFile(File blog) {
        String title = null;

        try {
            title = Files.readAllLines(blog.toPath()).getFirst();

            Matcher match = HTML_LINE_VALUE_PATTERN.matcher(title);

            if (match.find()) {
                title = match.group(1);
            }
        } catch (IOException | IndexOutOfBoundsException | NoSuchElementException e) {
            Log.error("Failed to get title of file " + blog.getName());
        }

        return title;
    }
}
