package xyz.mijaljevic.scheduler;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.cache.BlogCache;
import xyz.mijaljevic.cache.BlogRenderer;
import xyz.mijaljevic.domain.dto.BlogMetadata;
import xyz.mijaljevic.domain.entity.Blog;
import xyz.mijaljevic.lifecycle.DirectoryProvisioner;
import xyz.mijaljevic.utils.FileUtils;
import xyz.mijaljevic.utils.MarkdownParser;
import xyz.mijaljevic.utils.Slugs;
import xyz.mijaljevic.web.WebPage;

/**
 * Scheduler that contains a scheduled method that runs every 5 minutes and
 * checks the {@link WatchKey} that was initialized during class creation. The
 * key monitors the creation/update/deletion of the blogs directory and
 * creates/updates/deletes entries in the {@link BlogCache} accordingly.
 */
@Startup
@ApplicationScoped
final class BlogScheduler {
    /**
     * The in-memory cache that is the single source of truth for blogs.
     */
    private final BlogCache blogCache;

    /**
     * Renders (and caches) the HTML body of a blog; invalidated on change.
     */
    private final BlogRenderer blogRenderer;

    /**
     * Provisions and exposes the watched blogs directory. Injecting it makes
     * directory creation a CDI dependency of this scheduler, so the directory
     * is guaranteed to exist before {@link #initBlogScheduler()} registers the
     * {@link WatchService} on it.
     */
    private final DirectoryProvisioner directoryProvisioner;

    /**
     * The blogs' directory, resolved from the provisioner in
     * {@link #initBlogScheduler()}.
     */
    private Path blogsDirectory;

    @Inject
    BlogScheduler(
            final BlogCache blogCache,
            final BlogRenderer blogRenderer,
            final DirectoryProvisioner directoryProvisioner) {
        this.blogCache = blogCache;
        this.blogRenderer = blogRenderer;
        this.directoryProvisioner = directoryProvisioner;
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
     * Furthermore, it also compares the cached blogs against the files to
     * check which cached blog has lost its file if any and then removes it
     * from the cache.
     * </p>
     */
    @PostConstruct
    void initBlogScheduler() {
        blogsDirectory = directoryProvisioner.blogsDirectory();

        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Log.fatal("Blog scheduler failed to initialize WatchService!", e);
            Quarkus.asyncExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher not available!");
        }

        try {
            watchKey = blogsDirectory.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            watchKeyValid = watchKey.isValid();
        } catch (IOException e) {
            Log.fatalf(e, "Blog scheduler failed to register a watch on '%s'!", blogsDirectory);
            Quarkus.asyncExit();
        }

        final File[] files = blogsDirectory.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        final List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeBlogFile(file);
            fileNames.add(file.getName());
        }

        for (Blog blog : blogCache.missing(fileNames)) {
            Log.warnf("Found blog without file. Deleting blog: %s", blog.getFileName());

            blogCache.removeByFileName(blog.getFileName());
            blogRenderer.invalidate(blog.getFileName());
        }

        WebPage.updateCacheControlHeaders();
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was
     * initialized during class creation. The key monitors the blogs directory
     * and this method creates/updates/deletes entries in the
     * {@link BlogCache} accordingly.
     */
    @Scheduled(identity = "blog_scheduler", every = "5m", delayed = "5s")
    void runBlogScheduler() {
        if (!watchKeyValid) {
            Log.fatal("BLOG - WatchKey NOT valid! Stopping scheduler!");

            return;
        }

        boolean changeOccurred = false;

        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("BLOG - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked")
            final WatchEvent<Path> ev = (WatchEvent<Path>) event;

            final Path filename = ev.context();

            final File file = blogsDirectory.resolve(filename).toFile();

            if (!isMarkdownFile(file)) {
                Log.warnf("BLOG - Not a markdown file: %s", file.getName());
                continue;
            }

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (!consumeBlogFile(file)) {
                    continue;
                }
            } else {
                final Blog blog = blogCache.byFileName(file.getName());

                if (blog != null) {
                    blogCache.removeByFileName(blog.getFileName());
                    blogRenderer.invalidate(blog.getFileName());
                    Log.infof("Successfully deleted blog of file: %s", file.getName());
                }
            }

            changeOccurred = true;
        }

        if (changeOccurred) {
            WebPage.updateCacheControlHeaders();
        }

        watchKeyValid = watchKey.reset();
    }

    /**
     * Consumes the provided {@link Blog} {@link File} and either updates or
     * creates a blog model depending on the state of the blog file against the
     * model in the cache. The title is derived from the file's front-matter
     * metadata when present, falling back to the first heading. The created and
     * updated timestamps come solely from the {@code Date}/{@code Updated}
     * metadata; a file without a parseable {@code Date} is rejected.
     *
     * @param file A blog file to consume.
     * @return False in case it failed to parse the file or the file carries no
     *         parseable {@code Date} metadata, and true if the process was
     *         successful.
     */
    private boolean consumeBlogFile(@Nonnull final File file) {
        final String fileName = file.getName();

        Blog blog = blogCache.byFileName(fileName);

        final boolean isNew = blog == null;

        if (isNew) {
            blog = new Blog();
            blog.setFileName(fileName);
        }

        final String oldHash = blog.getHash();

        final String hash;

        try {
            hash = FileUtils.hashFile(file);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.errorf(e, "Failed to hash file %s with algorithm %s", fileName, Website.HASH_ALGORITHM);
            return false;
        }

        if (!isNew && hash.equals(oldHash)) {
            return true;
        }

        final BlogMetadata metadata = MarkdownParser.parseMetadata(file);

        final String title = metadata.title() != null && !metadata.title().isBlank()
                ? metadata.title()
                : MarkdownParser.getTitleFromFile(file);

        blog.setTitle(title);
        blog.setSlug(Slugs.slugify(title));
        blog.setAuthor(metadata.author());
        blog.setTags(metadata.tags());
        blog.setHash(hash);

        if (metadata.date() == null) {
            Log.errorf("Blog file %s has no parseable Date metadata; skipping.", fileName);
            return false;
        }

        blog.setCreated(metadata.date().atStartOfDay());
        blog.setUpdated(metadata.updated() != null ? metadata.updated().atStartOfDay() : null);

        blogCache.put(blog);

        blogRenderer.invalidate(fileName);

        if (isNew) {
            Log.infof("Successfully created blog for file: %s", fileName);
        } else {
            Log.infof("Successfully updated blog for file: %s", fileName);
        }

        return true;
    }

    /**
     * Checks if the provided file is a Markdown file by checking its
     * extension.
     *
     * @param file The file to check.
     * @return True if the file is a Markdown file and false otherwise.
     */
    private static boolean isMarkdownFile(final File file) {
        if (file == null || !file.isFile()) {
            return false;
        }

        final String fileName = file.getName().toLowerCase();

        return fileName.endsWith(".md");
    }
}
