package org.limelightcreatives.dashboard;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;

/**
 * A real PostgreSQL started from Java (embedded-postgres runs the actual PostgreSQL binaries,
 * which come from Maven), so tests and local runs need nothing installed and no Docker.
 */
public final class TestPostgres {

    private static final String EXTERNAL = System.getenv("TEST_DATABASE_URL");
    private static EmbeddedPostgres forTests;

    private TestPostgres() {}

    /** A throwaway database for the test run, or TEST_DATABASE_URL if that's set. */
    public static synchronized String jdbcUrl() {
        if (EXTERNAL != null) return EXTERNAL;
        if (forTests == null) forTests = start(EmbeddedPostgres.builder());
        return forTests.getJdbcUrl("postgres", "postgres");
    }

    /** A database that keeps its data between runs, in {@code dataDirectory}, on a fixed port. */
    public static String persistent(Path dataDirectory, int port) {
        var postgres = start(EmbeddedPostgres.builder()
                .setDataDirectory(dataDirectory)
                .setCleanDataDirectory(false)
                .setPort(port));
        return postgres.getJdbcUrl("postgres", "postgres");
    }

    private static EmbeddedPostgres start(EmbeddedPostgres.Builder builder) {
        try {
            EmbeddedPostgres postgres = builder.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    postgres.close();
                } catch (IOException ignored) {
                    // shutting down anyway
                }
            }));
            return postgres;
        } catch (IOException e) {
            throw new UncheckedIOException("Couldn't start PostgreSQL", e);
        }
    }
}
