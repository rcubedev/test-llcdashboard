package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** An event dashboard's Home tab. */
@AutoConfigureMockMvc
class EventHomeTest extends DatabaseTest {

    private static final String PAGE = "/participant/film-fest";

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired MilestoneSubmissions submissions;

    private String teamId;
    private Visitor sam;

    @BeforeEach
    void aTeam() {
        teams.create("sam", "Gaffers");
        teamId = teams.of("sam").orElseThrow().id();
        sam = new Visitor(mvc).as(SignedIn.participant("sam"));
    }

    @Test
    void withoutATeam() throws Exception {
        assertThat(new Visitor(mvc).as(SignedIn.participant("loner")).get(PAGE).getContentAsString())
                .contains("Join a team first", "Welcome to your dashboard!");
    }

    @Test
    void upNextIsTheFirstMilestone() throws Exception {
        String page = sam.get(PAGE).getContentAsString();
        assertThat(page).contains("Showing progress for <strong>Gaffers</strong>", "Up next", "Milestone 1 of 3",
                "🕒 Day 1, morning", ">SUBMIT</a>", "0/3 approved · 0/1 bonus", "Nothing waiting on an organiser.",
                "1 / 3 members", "Manage team", "Updated ")
                .contains("style=\"width: 0%\"")
                .doesNotContain("Join a team first");
    }

    @Test
    void submittedMilestonesWaitForReview() throws Exception {
        submissions.record(teamId, "storyboard", "Storyboard", "s1", "[]");
        String page = sam.get(PAGE).getContentAsString();
        assertThat(page).contains("An organiser is reviewing this.", "Awaiting review").doesNotContain(">SUBMIT</a>");
        assertThat(page).containsPattern("In review</p>\\s*<p[^>]*>1</p>");
    }

    @Test
    void approvingMovesOnAndFillsTheBar() throws Exception {
        submissions.record(teamId, "storyboard", "Storyboard", "s1", "[]");
        db.sql("UPDATE milestone_submissions SET status = 'approved'").update();
        String page = sam.get(PAGE).getContentAsString();
        assertThat(page).contains("Milestone 2 of 3", "1/3 approved", "style=\"width: 33%\"", ">✓</span>");
    }

    @Test
    void allDone() throws Exception {
        for (String id : new String[]{"storyboard", "shoot", "final"}) submissions.record(teamId, id, id, id, "[]");
        db.sql("UPDATE milestone_submissions SET status = 'approved'").update();
        assertThat(sam.get(PAGE).getContentAsString()).contains("All done!", "Every milestone is approved.",
                "Bonus milestones are still open", "View bonus milestones", "style=\"width: 100%\"");
    }
}
