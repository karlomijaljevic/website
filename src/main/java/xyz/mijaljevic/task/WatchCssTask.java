package xyz.mijaljevic.task;

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

import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import xyz.mijaljevic.ExitCodes;
import xyz.mijaljevic.Website;
import xyz.mijaljevic.model.StaticFileService;
import xyz.mijaljevic.model.entity.StaticFile;
import xyz.mijaljevic.model.entity.StaticFileType;

/**
 * Class contains a scheduled method that runs every 5 minutes and checks the
 * {@link WatchKey} that was initialized during class creation. The key monitors
 * the creation/update/deletion of the css directory and creates/updates/deletes
 * entries in the DB accordingly.
 */
@ApplicationScoped
final class WatchCssTask {
    @Inject
    StaticFileService staticFileService;

    /**
     * Holds the reference to the css directory {@link WatchKey}.
     */
    private static WatchKey WatchKey = null;

    /**
     * True when the {@link WatchKey} is valid and false otherwise.
     */
    private static boolean WatchKeyValid = false;

    /**
     * Initializes the class {@link WatchKey} variable <i>WatchKey</i> and performs
     * the initial css directory check up for new or updated files. It also compares
     * the database css entities against the files to check which DB css entity has
     * lost its file if any and then removes the entity from the DB.
     */
    @PostConstruct
    void initWatchCssTask() {
        WatchService watcher = null;

        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            ExitCodes.WATCH_CSS_TASK_WATCH_SERVICE_FAILED.logAndExit();
        }

        if (watcher == null) {
            throw new RuntimeException("Watcher service not available");
        }

        try {
            WatchKey = Website.getCssDirectory().register(watcher, StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

            WatchKeyValid = WatchKey.isValid();
        } catch (IOException e) {
            ExitCodes.WATCH_CSS_TASK_WATCH_KEY_FAILED.logAndExit();
        }

        File[] files = Website.getCssDirectory().toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeCssFile(file);
            fileNames.add(file.getName());
        }

        List<StaticFile> staticFiles = staticFileService.listAllMissingFiles(fileNames, StaticFileType.CSS);

        for (StaticFile staticFile : staticFiles) {
            staticFileService.deleteStaticFile(staticFile);

            Log.info("Found css entity without file. Deleting it. File: " + staticFile.getName());
        }
    }

    /**
     * Runs every 5 minutes and checks the {@link WatchKey} that was initialized
     * during class creation. The key monitors the creation/update/deletion of the
     * css directory and this method creates/updates/deletes entries in the DB
     * accordingly.
     */
    @Scheduled(identity = "watch_css_task", every = "5m", delayed = "5s")
    void runWatchCssTask() {
        if (!WatchKeyValid) {
            Log.fatal("WatchKey for the WatchCssTask is not valid. Exiting watch_css_task task!");

            return;
        }

        for (WatchEvent<?> event : WatchKey.pollEvents()) {
            WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("OVERFLOW event occurred while watching the css directory!");

                continue;
            }

            @SuppressWarnings("unchecked")
            WatchEvent<Path> ev = (WatchEvent<Path>) event;

            Path filename = ev.context();

            File file = Website.getCssDirectory().resolve(filename).toFile();

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                if (consumeCssFile(file)) {
                    StaticFile staticFile = staticFileService.findFileByName(file.getName());

                    if (staticFileService.deleteStaticFile(staticFile)) {
                        Website.STATIC_CACHE.remove(staticFile.getName());
                        Log.info("Successfully deleted css of file: " + file.getName());
                    }
                }
            }

            WatchKeyValid = WatchKey.reset();
        }
    }

    /**
     * Consumes the provided {@link StaticFile} {@link File} and either updates or
     * creates a css entity depending on the state of the css file against the
     * entity in the DB.
     *
     * @param file A css file to consume.
     * @return False in case it failed to parse the file and true if the process was
     * successful.
     */
    private boolean consumeCssFile(File file) {
        boolean isNew = false;
        StaticFile staticFile = staticFileService.findFileByName(file.getName());

        if (staticFile == null) {
            staticFile = new StaticFile();
            isNew = true;
        }

        String oldHash = staticFile.getHash();

        try {
            String hash = TaskHelper.hashFile(file);
            staticFile.setHash(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.error("Failed to hash file " + staticFile.getName() + " with algorithm " + Website.HASH_ALGORITHM);
            return false;
        }

        if (isNew) {
            staticFile.setName(file.getName());
            staticFile.setType(StaticFileType.CSS);
            staticFileService.createStaticFile(staticFile);
            staticFile = staticFileService.findFileByName(file.getName());
            Log.info("Successfully created css entity for file: " + file.getName());
        } else {
            // In case no actual file changes have occurred.
            if (!staticFile.getHash().equals(oldHash)) {
                staticFile = staticFileService.updateStaticFile(staticFile);
                Log.info("Successfully updated css entity for file: " + file.getName());
            }
        }

        Website.STATIC_CACHE.put(staticFile.getName(), staticFile);

        return true;
    }
}
