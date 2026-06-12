package xyz.mijaljevic.cache;

import jakarta.enterprise.context.ApplicationScoped;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFileType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application scoped in-memory index of {@link StaticFile} models; this cache
 * is the single source of truth for static files. Lookups by name are O(1).
 */
@ApplicationScoped
public class StaticFileCache {
    /**
     * Index keyed by static file name.
     */
    private final Map<String, StaticFile> byName = new ConcurrentHashMap<>();

    /**
     * Inserts or updates the provided static file in the index.
     *
     * @param staticFile The {@link StaticFile} to store.
     */
    public void put(final StaticFile staticFile) {
        byName.put(staticFile.getName(), staticFile);
    }

    /**
     * Removes the static file with the provided name from the index.
     *
     * @param name The name of the static file to remove.
     */
    public void removeByName(final String name) {
        byName.remove(name);
    }

    /**
     * Finds a static file by its name.
     *
     * @param name The name to search for.
     * @return The matching {@link StaticFile}, or {@code null} if none exists.
     */
    public StaticFile byName(final String name) {
        return byName.get(name);
    }

    /**
     * Returns all cached static files of the provided type whose names are not
     * contained in the provided collection. Used by the scheduler to detect
     * files whose backing file was removed.
     *
     * @param names The names that currently exist on disk.
     * @param type  The {@link StaticFileType} to filter by.
     * @return The cached {@link StaticFile} models of the provided type missing
     * from the provided collection.
     */
    public List<StaticFile> missing(
            final Collection<String> names,
            final StaticFileType type
    ) {
        return byName.values()
                .stream()
                .filter(staticFile -> staticFile.getType() == type && !names.contains(staticFile.getName()))
                .toList();
    }
}
