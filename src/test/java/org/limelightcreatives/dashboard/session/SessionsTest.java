package org.limelightcreatives.dashboard.session;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** Sessions are kept in the dashboard's own tables, so they survive restarts and never mix with auth's. */
class SessionsTest extends DatabaseTest {

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Autowired SessionRepository sessions;

    @Test
    void keptInTheDashboardsOwnTable() {
        Session session = sessions.createSession();
        session.setAttribute("hello", "world");
        sessions.save(session);

        assertThat(db.sql("SELECT count(*) FROM dashboard_session").query(Integer.class).single()).isOne();
        assertThat(sessions.findById(session.getId()).<String>getAttribute("hello")).isEqualTo("world");
    }

    @Test
    void lastThirtyDays() {
        assertThat(((Session) sessions.createSession()).getMaxInactiveInterval()).isEqualTo(Duration.ofDays(30));
    }
}
