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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class TaskUtilsTest {
    @TempDir
    Path tempDir;

    @Test
    @DisplayName("hashFile of an empty file returns the known SHA-256 digest")
    void hashFile_emptyFile_returnsKnownSha256() throws Exception {
        File file = Files.createFile(tempDir.resolve("empty.bin")).toFile();

        // SHA-256 of the empty byte sequence (standard, well-known test vector).
        assertThat(TaskUtils.hashFile(file)).isEqualTo(
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );
    }

    @Test
    @DisplayName("hashFile of the bytes \"abc\" returns the known SHA-256 digest")
    void hashFile_knownContent_returnsKnownSha256() throws Exception {
        Path path = tempDir.resolve("abc.bin");
        Files.write(path, "abc".getBytes(StandardCharsets.UTF_8));

        // SHA-256 of "abc" (standard, well-known test vector).
        assertThat(TaskUtils.hashFile(path.toFile())).isEqualTo(
                "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        );
    }

    @Test
    @DisplayName("hashFile is deterministic for identical content")
    void hashFile_sameContent_producesSameHash() throws Exception {
        Path a = tempDir.resolve("a.txt");
        Path b = tempDir.resolve("b.txt");
        Files.write(a, "same payload".getBytes(StandardCharsets.UTF_8));
        Files.write(b, "same payload".getBytes(StandardCharsets.UTF_8));

        assertThat(TaskUtils.hashFile(a.toFile()))
                .isEqualTo(TaskUtils.hashFile(b.toFile()));
    }

    @Test
    @DisplayName("hashFile rejects a null file")
    void hashFile_null_throwsNpe() {
        assertThatNullPointerException()
                .isThrownBy(() -> TaskUtils.hashFile(null));
    }
}
