package org.limelightcreatives.dashboard.project;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** After SUBMISSIONS_CLOSE_AT, projects stay as they are. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "dashboard.submissions-close-at=2026-01-01T00:00:00+11:00")
class ClosedSubmissionsTest extends DatabaseTest {

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired Projects projects;

    @Test
    void projectsCantChange() throws Exception {
        teams.create("sam", "Gaffers");
        assertThatThrownBy(() -> projects.save("sam", "t", "l", "https://x.com")).hasMessage("Submissions are closed");

        String page = new Visitor(mvc).as(SignedIn.participant("sam")).get("/participant/film-fest/project?edit=true").getContentAsString();
        assertThat(page).contains("Submissions are closed").doesNotContain("Save project", "href=\"?edit=true\"");
    }
}
