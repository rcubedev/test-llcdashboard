package org.limelightcreatives.dashboard;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base for tests that need the real database (PostgreSQL, like production), started by
 * {@link TestPostgres}. Every test starts with empty tables, Google Sheets as {@link FakeSpreadsheets} has
 * them, and the clock at {@link TestClock#START}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeans.class)
public abstract class DatabaseTest {

    /** Every table the dashboard keeps data in, emptied before each test. */
    static final String TABLES = "teams, team_members, submissions, milestone_submissions, resources, dashboard_session";

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", TestPostgres::jdbcUrl);
    }

    @Autowired
    protected JdbcClient db;

    @Autowired
    protected FakeSpreadsheets sheets;

    @Autowired
    protected TestClock clock;

    @BeforeEach
    void emptyTables() {
        this.db.sql("TRUNCATE " + TABLES + " CASCADE").update();
        this.sheets.reset();
        this.clock.set(TestClock.START);
    }
}
