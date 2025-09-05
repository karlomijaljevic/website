package xyz.mijaljevic.task;

import io.quarkus.logging.Log;
import io.quarkus.runtime.Quarkus;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.StaticFileService;
import xyz.mijaljevic.model.entity.StaticFile;
import xyz.mijaljevic.model.entity.StaticFile.Type;
import xyz.mijaljevic.utils.TaskUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class contains a scheduled method that runs every 5 minutes and checks the
 * {@link WatchKey} that was initialized during class creation. The key
 * monitors the images directory and creates/updates/deletes entries in the DB
 * accordingly.
 */
@ApplicationScoped
final class WatchImagesTask {
    /**
     * The service that handles the {@link StaticFile} entity CRUD operations.
     */
    @Inject
    StaticFileService staticFileService;

    /**
     * The path to the images' directory.
     */
    @ConfigProperty(
            name = "application.images-directory",
            defaultValue = "static/images"
    )
    String imagesDirectoryPath;

    /**
     * Holds the reference to the images directory {@link WatchKey}.
     */
    private static WatchKey WatchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean WatchKeyValid = false;

    /**
     * Initializes the class {@link WatchKey} variable <i>WatchKey</i> and
     * performs the initial images directory check up for new or updated files.
     * It also compares the database images against the files to check which DB
     * image has lost its file if any and then removes the entity from the DB.
     */
    @PostConstruct
    void initWatchImagesTask() {
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

        Path imagesPath = Paths.get(imagesDirectoryPath);

        try {
            WatchKey = imagesPath.register(
                    watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
            );

            WatchKeyValid = WatchKey.isValid();
        } catch (IOException e) {
            Log.fatal("Failed to register images directory with WatchService", e);
            Quarkus.asyncExit();
        }

        File[] files = imagesPath.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeImageFile(file);
            fileNames.add(file.getName());
        }

        List<StaticFile> staticFiles = staticFileService.listAllMissingFiles(
                fileNames,
                Type.IMAGE
        );

        for (StaticFile file : staticFiles) {
            staticFileService.deleteStaticFile(file);

            Log.info("Found image without file. Deleting file: " + file.getName());
        }
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was
     * initialized during class creation. The key monitors the images directory
     * and this method creates/updates/deletes entries in the DB accordingly.
     */
    @Scheduled(
            identity = "watch_images_task",
            every = "5m",
            delayed = "5s"
    )
    void runWatchImagesTask() {
        if (!WatchKeyValid) {
            Log.fatal("IMAGE - WatchKey NOT valid. Stopping task!");
            return;
        }

        for (WatchEvent<?> event : WatchKey.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("IMAGE - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;

            Path filename = ev.context();

            Path imagesPath = Paths.get(imagesDirectoryPath);

            File file = imagesPath.resolve(filename).toFile();

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                consumeImageFile(file);
            } else {
                StaticFile staticFile = staticFileService.findFileByName(file.getName());

                if (staticFile != null && staticFileService.deleteStaticFile(staticFile)) {
                    Website.STATIC_CACHE.remove(staticFile.getName());
                    Log.info("Successfully deleted image of file: " + file.getName());
                }
            }
        }

        WatchKeyValid = WatchKey.reset();
    }

    /**
     * Consumes the provided {@link StaticFile} {@link File} and either updates or
     * creates an image entity depending on the state of the image file against the
     * entity in the DB.
     *
     * @param file Image file to consume.
     */
    private void consumeImageFile(File file) {
        boolean isNew = false;
        StaticFile staticFile = staticFileService.findFileByName(file.getName());

        if (staticFile == null) {
            staticFile = new StaticFile();
            isNew = true;
        }

        String oldHash = staticFile.getHash();

        try {
            String hash = TaskUtils.hashFile(file);
            staticFile.setHash(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.error("Failed to hash file "
                    + staticFile.getName()
                    + " with algorithm "
                    + Website.HASH_ALGORITHM
            );
        }

        if (isNew) {
            staticFile.setName(file.getName());
            staticFile.setType(Type.IMAGE);
            staticFileService.createStaticFile(staticFile);
            staticFile = staticFileService.findFileByName(file.getName());
            Log.info("Successfully created image for file: " + file.getName());
        } else {
            // In case no actual file changes have occurred.
            if (!staticFile.getHash().equals(oldHash)) {
                staticFile = staticFileService.updateStaticFile(staticFile);
                Log.info("Successfully updated image for file: " + file.getName());
            }
        }

        Website.STATIC_CACHE.put(staticFile.getName(), staticFile);
    }
}
