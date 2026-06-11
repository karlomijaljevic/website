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

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.mijaljevic.test.MissingBlogsDirectoryTestResource;

import java.nio.file.Files;
import java.nio.file.Path;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression test for the Task 9 startup race: when the configured blogs
 * directory is absent at boot, {@link DirectoryProvisioner} must create it
 * before the {@code @Startup} blog scheduler registers its watch, so the
 * application starts (no {@code asyncExit}) and serves requests.
 */
@QuarkusTest
@QuarkusTestResource(value = MissingBlogsDirectoryTestResource.class, restrictToAnnotatedClass = true)
class DirectoryProvisionerStartupTest {
    /**
     * The blogs directory the test resource pointed at an absent path before
     * boot.
     */
    @ConfigProperty(name = "application.blogs-directory")
    String blogsDirectoryPath;

    @Test
    @DisplayName("An absent blogs directory is created during application startup")
    void absentBlogsDirectory_isCreatedAtStartup() {
        final Path blogsDirectory = Path.of(blogsDirectoryPath);

        assertThat(Files.exists(blogsDirectory)).isTrue();
        assertThat(Files.isDirectory(blogsDirectory)).isTrue();
    }

    @Test
    @DisplayName("The application serves the home page after provisioning an absent blogs directory")
    void appServesHomePage_afterProvisioningAbsentDirectory() {
        given()
                .when().get("/")
                .then().statusCode(200);
    }
}
