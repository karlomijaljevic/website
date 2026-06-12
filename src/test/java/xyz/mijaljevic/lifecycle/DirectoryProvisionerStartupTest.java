package xyz.mijaljevic.lifecycle;

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
