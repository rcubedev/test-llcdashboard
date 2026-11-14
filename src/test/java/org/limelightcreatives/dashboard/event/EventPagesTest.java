package org.limelightcreatives.dashboard.event;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** /participant/{eventId}: the dashboard of an open event, and the cards for the others. */
@AutoConfigureMockMvc
class EventPagesTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    private Visitor participant() {
        return new Visitor(mvc).as(SignedIn.participant("u1"));
    }

    @Test
    void anOpenEventHasItsTabsInItsColours() throws Exception {
        var page = participant().get("/participant/film-fest");
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getContentAsString())
                .contains("--accent:#123456;--accent-secondary:#123456")
                .contains("href=\"/participant/film-fest\"", "href=\"/participant/film-fest/milestones\"",
                        "href=\"/participant/film-fest/project\"", "href=\"/participant/film-fest/equipment\"",
                        "href=\"/participant/film-fest/team\"", "href=\"/participant/film-fest/resources\"")
                .containsPattern("aria-current=\"page\"[^>]*>\\s*<svg[^>]*>[\\s\\S]*?<span>Home</span>")
                .contains("<title>Film Fest - Limelight Creatives</title>");
    }

    @Test
    void anEventThatIsntThereSaysSo() throws Exception {
        var page = participant().get("/participant/nope");
        assertThat(page.getStatus()).isEqualTo(404);
        assertThat(page.getContentAsString()).contains("This event doesn’t exist", "\"nope\"", "Go back home",
                "Sam Lee"); // still the normal top bar
    }

    @Test
    void anInactiveEventIsntThere() throws Exception {
        assertThat(participant().get("/participant/retired").getStatus()).isEqualTo(404);
    }

    @Test
    void aLockedEventIsClosed() throws Exception {
        var page = participant().get("/participant/closed");
        assertThat(page.getStatus()).isEqualTo(200);
        assertThat(page.getContentAsString()).contains("This event isn’t open yet", "Closed Event")
                .doesNotContain("href=\"/participant/closed/team\"");
    }
}
