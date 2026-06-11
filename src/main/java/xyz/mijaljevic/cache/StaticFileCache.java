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

package xyz.mijaljevic.cache;

import jakarta.enterprise.context.ApplicationScoped;
import xyz.mijaljevic.domain.entity.StaticFile;
import xyz.mijaljevic.domain.entity.StaticFile.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Application scoped in-memory index of {@link StaticFile} models. Replaces the
 * former global static map and the H2/Hibernate persistence layer; this cache
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
     * @param type  The {@link Type} to filter by.
     * @return The cached {@link StaticFile} models of the provided type missing
     * from the provided collection.
     */
    public List<StaticFile> missing(final Collection<String> names, final Type type) {
        return byName.values()
                .stream()
                .filter(staticFile -> staticFile.getType() == type && !names.contains(staticFile.getName()))
                .toList();
    }
}
