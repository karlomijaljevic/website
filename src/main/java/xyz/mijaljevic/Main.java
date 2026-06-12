package xyz.mijaljevic;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

/**
 * Main class of the application.
 */
@QuarkusMain
public final class Main {
    static void main(final String... args) {
        Quarkus.run(Website.class, args);
    }
}
