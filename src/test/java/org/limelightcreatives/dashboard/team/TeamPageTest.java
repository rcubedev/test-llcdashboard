package org.limelightcreatives.dashboard.team;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** The Team tab: create, share the code, join, leave, and what went wrong. */
@AutoConfigureMockMvc
class TeamPageTest extends DatabaseTest {

    private static final String PAGE = "/participant/film-fest/team";

    @Autowired MockMvc mvc;
    @Autowired Teams teams;

    @Test
    void createShareJoinAndLeave() throws Exception {
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
        assertThat(sam.get(PAGE).getContentAsString()).contains("Join a team", "Create a team");

        var created = sam.post(PAGE + "/create", "name", "The Gaffers");
        assertThat(created.getRedirectedUrl()).isEqualTo(PAGE);
        String code = teams.of("sam").orElseThrow().joinCode();
        assertThat(sam.get(PAGE).getContentAsString()).contains("You’re on", "The Gaffers", "1 / 3 members", code,
                "data-copy=\"" + code + "\"", "Leave team");

        var alex = new Visitor(mvc).as(SignedIn.participant("alex"));
        alex.post(PAGE + "/join", "code", code.toLowerCase());
        assertThat(alex.get(PAGE).getContentAsString()).contains("The Gaffers", "2 / 3 members");

        alex.post(PAGE + "/leave");
        assertThat(teams.of("alex")).isEmpty();
        assertThat(alex.get(PAGE).getContentAsString()).contains("Join a team");
    }

    @Test
    void whatWentWrongIsShownOnceWithWhatTheyTyped() throws Exception {
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
        var refused = sam.follow(sam.post(PAGE + "/join", "code", "NOPE22"));
        assertThat(refused.getContentAsString()).contains("No team with that code", "value=\"NOPE22\"");
        // gone after a refresh
        assertThat(sam.get(PAGE).getContentAsString()).doesNotContain("No team with that code");
    }

    @Test
    void whatPeopleTypeIsEscaped() throws Exception {
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
        sam.post(PAGE + "/create", "name", "<b>Gaffers</b> \"& co\"");
        String page = sam.get(PAGE).getContentAsString();
        assertThat(page).contains("&lt;b&gt;Gaffers&lt;/b&gt; \"&amp; co\"").doesNotContain("<b>Gaffers");
    }

    @Test
    void theFormsNeedTheirToken() throws Exception {
        var page = new Visitor(mvc).as(SignedIn.participant("sam")).get(PAGE).getContentAsString();
        assertThat(page).contains("name=\"_csrf\"");
    }
}
