package org.limelightcreatives.dashboard;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@TestPropertySource(properties = "dashboard.submissions-close-at=2026-11-01T23:59:00+11:00")
class DashboardPropertiesTest extends DatabaseTest {

    @Autowired DashboardProperties properties;

    @Test
    void submissionsCloseAtTheGivenTime() {
        assertThat(properties.submissionsClosed(Instant.parse("2026-11-01T12:58:00Z"))).isFalse();
        assertThat(properties.submissionsClosed(Instant.parse("2026-11-01T13:00:00Z"))).isTrue();
    }

    @Test
    void emptyMeansNever() {
        var never = new DashboardProperties(properties.baseUrl(), properties.auth(), false, null, null, null, null);
        assertThat(never.submissionsClosed(Instant.MAX)).isFalse();
    }
}
