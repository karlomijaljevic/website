package xyz.mijaljevic.scheduler;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.Startup;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.cache.StaticFileCache;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFileType;
import xyz.mijaljevic.lifecycle.DirectoryProvisioner;
import xyz.mijaljevic.utils.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler that contains a scheduled method that runs every 5 minutes and
 * checks the
 * {@link WatchKey} that was initialized during class creation. The key
 * monitors the images directory and creates/updates/deletes entries in the
 * {@link StaticFileCache} accordingly.
 */
@Startup
@ApplicationScoped
final class ImageScheduler {
    /**
     * The in-memory cache that is the single source of truth for static files.
     */
    private final StaticFileCache staticFileCache;

    /**
     * Provisions and exposes the watched images directory. Injecting it makes
     * directory creation a CDI dependency of this scheduler, so the directory
     * is guaranteed to exist before {@link #initImageScheduler()} registers the
     * {@link WatchService} on it.
     */
    private final DirectoryProvisioner directoryProvisioner;

    /**
     * The images' directory, resolved from the provisioner in
     * {@link #initImageScheduler()}.
     */
    private Path imagesDirectory;

    @Inject
    ImageScheduler(
            final StaticFileCache staticFileCache,
            final DirectoryProvisioner directoryProvisioner
    ) {
        this.staticFileCache = staticFileCache;
        this.directoryProvisioner = directoryProvisioner;
    }

    /**
     * Holds the reference to the images directory {@link WatchKey}.
     */
    private static WatchKey watchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean watchKeyValid = false;

    /**
     * Initializes the class {@link WatchKey} variable <i>watchKey</i> and
     * performs the initial images directory check up for new or updated files.
     * It also compares the cached images against the files to check which
     * cached image has lost its file if any and then removes it from the cache.
     */
    @PostConstruct
    void initImageScheduler() {
        imagesDirectory = directoryProvisioner.imagesDirectory();

        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            Log.fatal("Failed to initialize WatchService for images directory", e);
            Quarkus.asyncExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher service not available");
        }

        try {
            watchKey = imagesDirectory.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            watchKeyValid = watchKey.isValid();
        } catch (IOException e) {
            Log.fatal("Failed to register images directory with WatchService", e);
            Quarkus.asyncExit();
        }

        final File[] files = imagesDirectory.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        final List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeImageFile(file);
            fileNames.add(file.getName());
        }

        for (StaticFile file : staticFileCache.missing(fileNames, StaticFileType.IMAGE)) {
            staticFileCache.removeByName(file.getName());

            Log.infof("Found image without file. Deleting image: %s", file.getName());
        }
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was
     * initialized during class creation. The key monitors the images directory
     * and this method creates/updates/deletes entries in the
     * {@link StaticFileCache} accordingly.
     */
    @Scheduled(
            identity = "image_scheduler",
            every = "5m",
            delayed = "5s"
    )
    void runImageScheduler() {
        if (!watchKeyValid) {
            Log.fatal("IMAGE - WatchKey NOT valid. Stopping scheduler!");
            return;
        }

        for (final WatchEvent<?> event : watchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("IMAGE - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked") final WatchEvent<Path> ev = (WatchEvent<Path>) event;

            final Path filename = ev.context();

            final File file = imagesDirectory.resolve(filename).toFile();

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                consumeImageFile(file);
            } else {
                final StaticFile staticFile = staticFileCache.byName(file.getName());

                if (staticFile != null) {
                    staticFileCache.removeByName(staticFile.getName());
                    Log.infof("Successfully deleted image of file: %s", file.getName());
                }
            }
        }

        watchKeyValid = watchKey.reset();
    }

    /**
     * Consumes the provided {@link StaticFile} {@link File} and either updates
     * or creates an image model depending on the state of the image file
     * against the model in the cache. If hashing the file fails the model is
     * left untouched and the method returns without storing a partially
     * updated model. The modified timestamp is derived from the file's
     * filesystem attributes.
     *
     * @param file Image file to consume.
     */
    private void consumeImageFile(@Nonnull final File file) {
        final String fileName = file.getName();

        StaticFile staticFile = staticFileCache.byName(fileName);

        final boolean isNew = staticFile == null;

        if (isNew) {
            staticFile = new StaticFile();
            staticFile.setName(fileName);
            staticFile.setType(StaticFileType.IMAGE);
        }

        final String oldHash = staticFile.getHash();

        final String hash;

        try {
            hash = FileUtils.hashFile(file);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.errorf(e, "Failed to hash file %s with algorithm %s", fileName, Website.HASH_ALGORITHM);
            return;
        }

        if (!isNew && hash.equals(oldHash)) {
            return;
        }

        staticFile.setHash(hash);

        try {
            final BasicFileAttributes attributes = Files.readAttributes(
                    file.toPath(),
                    BasicFileAttributes.class
            );

            staticFile.setModified(LocalDateTime.ofInstant(
                    attributes.lastModifiedTime().toInstant(),
                    Website.TIME_ZONE
            ));
        } catch (IOException e) {
            Log.errorf(e, "Failed to read file attributes for %s", fileName);
            return;
        }

        staticFileCache.put(staticFile);

        if (isNew) {
            Log.infof("Successfully created image for file: %s", fileName);
        } else {
            Log.infof("Successfully updated image for file: %s", fileName);
        }
    }
}
