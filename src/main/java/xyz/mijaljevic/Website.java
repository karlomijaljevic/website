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

package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.QuarkusApplication;

import java.time.ZoneId;

/**
 * Application class. Implements the {@link QuarkusApplication} interface.
 * Contains the application "<i>global</i>" constants.
 */
public final class Website implements QuarkusApplication {
    /**
     * Limit of latest blogs to display on the Home page and RSS feed.
     */
    public static final int NUMBER_OF_BLOGS_TO_DISPLAY = 8;

    /**
     * Time zone used by the website for clients.
     */
    public static final ZoneId TIME_ZONE = ZoneId.of("UTC");

    /**
     * Hash algorithm used by the website for file and Etag hashing.
     */
    public static final String HASH_ALGORITHM = "SHA-256";

    @Override
    public int run(final String... args) {
        Quarkus.waitForExit();

        return 0;
    }
}
