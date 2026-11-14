package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class MilestonesPageTest extends DatabaseTest {

    private static final String PAGE = "/participant/film-fest/milestones";

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired MilestoneSubmissions submissions;

    @Test
    void withoutATeam() throws Exception {
        assertThat(new Visitor(mvc).as(SignedIn.participant("loner")).get(PAGE).getContentAsString())
                .contains("Join a team first", "Milestones are submitted per team");
    }

    @Test
    void theFirstIsOpenTheRestWaitTheirTurn() throws Exception {
        teams.create("sam", "Gaffers");
        String teamId = teams.of("sam").orElseThrow().id();
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));

        String page = sam.get(PAGE).getContentAsString();
        assertThat(page).contains("Submitting as <strong>Gaffers</strong>", "Storyboard", "Plan every shot.", "🏆 Stickers",
                "🔒 Unlocks after “Storyboard” is approved.", "Helping Hand", "Bonus",
                "href=\"https://forms.example/t/film-fest?milestoneId=storyboard&amp;milestone=Storyboard&amp;teamId=" + teamId + "&amp;team=Gaffers\"");
        // the first one and the bonus one can be submitted
        assertThat(page.split(">SUBMIT</a>", -1)).hasSize(3);

        submissions.record(teamId, "storyboard", "Storyboard", "s1", "[]");
        assertThat(sam.get(PAGE).getContentAsString()).contains("Awaiting review").containsOnlyOnce(">SUBMIT</a>");

        db.sql("UPDATE milestone_submissions SET status = 'approved'").update();
        assertThat(sam.get(PAGE).getContentAsString()).contains("Approved", "🔒 Unlocks after “Shoot” is approved.")
                .doesNotContain("Unlocks after “Storyboard”");
    }
}
