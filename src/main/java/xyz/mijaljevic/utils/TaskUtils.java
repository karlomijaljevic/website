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

package xyz.mijaljevic.utils;

import xyz.mijaljevic.Website;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Functional helper class for file related operations.
 */
public final class TaskUtils {
    /**
     * Creates a {@link String} hash from the provided file. Uses the algorithm
     * specified by the <i>FILE_HASH_ALGO</i> variable.
     *
     * @param file A {@link File} to hash
     * @return Returns the {@link String} <i>FILE_HASH_ALGO</i> hash of the
     * provided file.
     * @throws IOException              in case it failed to read the data of
     *                                  the provided file.
     * @throws NoSuchAlgorithmException in case it failed to find the algorithm
     *                                  specified by the <i>FILE_HASH_ALGO</i>
     *                                  variable.
     * @throws NullPointerException     if {@code file} is null.
     */
    public static String hashFile(
            final File file
    ) throws IOException, NoSuchAlgorithmException {
        Objects.requireNonNull(file, "file must not be null");

        byte[] data = Files.readAllBytes(file.toPath());

        byte[] hash = MessageDigest.getInstance(Website.HASH_ALGORITHM)
                .digest(data);

        return new BigInteger(1, hash).toString(16);
    }
}
