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
import xyz.mijaljevic.domain.service.StaticFileService;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFile.Type;
import xyz.mijaljevic.utils.TaskUtils;

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
    private final StaticFileService staticFileService;

    /**
     * The path to the images' directory.
     */
    private final String imagesDirectoryPath;

    @Inject
    WatchImagesTask(
            final StaticFileService staticFileService,
            @ConfigProperty(
                    name = "application.images-directory",
                    defaultValue = "static/images"
            ) final String imagesDirectoryPath
    ) {
        this.staticFileService = staticFileService;
        this.imagesDirectoryPath = imagesDirectoryPath;
    }

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

        final Path imagesPath = Paths.get(imagesDirectoryPath);

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

        final File[] files = imagesPath.toFile().listFiles();

        if (files == null) {
            throw new RuntimeException("File list not available!");
        }

        final List<String> fileNames = new ArrayList<>();

        for (File file : files) {
            consumeImageFile(file);
            fileNames.add(file.getName());
        }

        final List<StaticFile> staticFiles = staticFileService.listAllMissingFiles(
                fileNames,
                Type.IMAGE
        );

        for (StaticFile file : staticFiles) {
            staticFileService.deleteStaticFile(file);

            Log.infof("Found image without file. Deleting file: %s", file.getName());
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

        for (final WatchEvent<?> event : WatchKey.pollEvents()) {
            final WatchEvent.Kind<?> kind = event.kind();

            if (kind == StandardWatchEventKinds.OVERFLOW) {
                Log.error("IMAGE - OVERFLOW event occurred!");
                continue;
            }

            @SuppressWarnings("unchecked")
            final WatchEvent<Path> ev = (WatchEvent<Path>) event;

            final Path filename = ev.context();

            final Path imagesPath = Paths.get(imagesDirectoryPath);

            final File file = imagesPath.resolve(filename).toFile();

            if (kind == StandardWatchEventKinds.ENTRY_CREATE || kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                consumeImageFile(file);
            } else {
                final StaticFile staticFile = staticFileService.findFileByName(file.getName());

                if (staticFile != null && staticFileService.deleteStaticFile(staticFile)) {
                    Website.STATIC_CACHE.remove(staticFile.getName());
                    Log.infof("Successfully deleted image of file: %s", file.getName());
                }
            }
        }

        WatchKeyValid = WatchKey.reset();
    }

    /**
     * Consumes the provided {@link StaticFile} {@link File} and either updates or
     * creates an image entity depending on the state of the image file against the
     * entity in the DB. If hashing the file fails the entity is left untouched
     * and the method returns without persisting a partially updated entity.
     *
     * @param file Image file to consume.
     */
    private void consumeImageFile(final File file) {
        boolean isNew = false;
        StaticFile staticFile = staticFileService.findFileByName(file.getName());

        if (staticFile == null) {
            staticFile = new StaticFile();
            isNew = true;
        }

        final String oldHash = staticFile.getHash();

        try {
            final String hash = TaskUtils.hashFile(file);
            staticFile.setHash(hash);
        } catch (NoSuchAlgorithmException | IOException e) {
            Log.errorf(e, "Failed to hash file %s with algorithm %s", staticFile.getName(), Website.HASH_ALGORITHM);
            return;
        }

        if (isNew) {
            staticFile.setName(file.getName());
            staticFile.setType(Type.IMAGE);
            staticFileService.createStaticFile(staticFile);
            staticFile = staticFileService.findFileByName(file.getName());
            Log.infof("Successfully created image for file: %s", file.getName());
        } else {
            // NOTE: Skip persisting when no actual file changes have occurred.
            if (!staticFile.getHash().equals(oldHash)) {
                staticFile = staticFileService.updateStaticFile(staticFile);
                Log.infof("Successfully updated image for file: %s", file.getName());
            }
        }

        Website.STATIC_CACHE.put(staticFile.getName(), staticFile);
    }
}
