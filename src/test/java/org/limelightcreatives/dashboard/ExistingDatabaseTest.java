package org.limelightcreatives.dashboard;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Starting on the database the old app used: its tables already exist (made by hand), and the auth app may
 * share it, with its own flyway_schema_history. Nothing is changed or lost.
 */
class ExistingDatabaseTest {

    @Test
    void startsOnTheOldAppsDatabaseSharedWithAuth() throws Exception {
        String url = TestPostgres.jdbcUrl();
        try (var connection = DriverManager.getConnection(url)) {
            var db = JdbcClient.create(new SingleConnectionDataSource(connection, true));
            db.sql("DROP SCHEMA IF EXISTS old_app CASCADE").update();
            db.sql("CREATE SCHEMA old_app").update();
            db.sql("SET search_path TO old_app").update();
            // The auth app's history table and a bit of its schema.
            db.sql("CREATE TABLE flyway_schema_history (installed_rank int PRIMARY KEY, version varchar(50), description varchar(200), "
                    + "type varchar(20), script varchar(1000), checksum int, installed_by varchar(100), installed_on timestamp DEFAULT now(), "
                    + "execution_time int, success boolean)").update();
            db.sql("INSERT INTO flyway_schema_history VALUES (1, '1', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', null, 'x', now(), 0, true)").update();
            db.sql("CREATE TABLE users (id text PRIMARY KEY, email text)").update();
            // The dashboard's tables as the old app had them, with a team in them.
            db.sql("CREATE TABLE teams (id uuid PRIMARY KEY DEFAULT gen_random_uuid(), name text NOT NULL, join_code text UNIQUE NOT NULL, "
                    + "owner_id text NOT NULL, created_at timestamptz DEFAULT now())").update();
            db.sql("CREATE TABLE team_members (user_id text PRIMARY KEY, team_id uuid REFERENCES teams ON DELETE CASCADE, joined_at timestamptz DEFAULT now())").update();
            db.sql("INSERT INTO teams (name, join_code, owner_id) VALUES ('Old Team', 'OLD123', 'u1')").update();

            Flyway.configure().dataSource(new SingleConnectionDataSource(connection, true)).schemas("old_app")
                    .table("dashboard_schema_history").baselineOnMigrate(true).baselineVersion("0").load().migrate();

            assertThat(db.sql("SELECT name FROM teams").query(String.class).single()).isEqualTo("Old Team");
            assertThat(db.sql("SELECT count(*) FROM resources").query(Integer.class).single()).isZero(); // created, as it was missing
            assertThat(db.sql("SELECT count(*) FROM flyway_schema_history").query(Integer.class).single()).isEqualTo(1); // auth's untouched
            assertThat(db.sql("SELECT string_agg(version, ',' ORDER BY installed_rank) FROM dashboard_schema_history").query(String.class).single())
                    .startsWith("0,1,"); // the baseline, then every migration
            db.sql("DROP SCHEMA old_app CASCADE").update();
        }
    }
}
