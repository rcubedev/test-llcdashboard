package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.milestone.MilestoneSubmissions.Decision;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** /staff/milestones: the review queue, decisions, and the progress table. */
@AutoConfigureMockMvc
class StaffMilestonesTest extends DatabaseTest {

    private static final String PAGE = "/staff/milestones";
    private static final String ANSWERS = """
            [{"id": "q1", "name": "Storyboard", "type": "FileUpload", "value": [{"url": "https://files.example/board.pdf"}]},
             {"id": "q2", "name": "Notes", "type": "LongAnswer", "value": "Line one\\nLine two"},
             {"id": "q3", "name": "Skipped", "type": "ShortAnswer", "value": null},
             {"id": "q4", "name": "Crew", "type": "Number", "value": 3}]
            """;

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired MilestoneSubmissions submissions;

    private String gaffers;
    private String grips;
    private Visitor staff;

    @BeforeEach
    void twoTeams() {
        teams.create("sam", "Gaffers");
        teams.join("alex", teams.of("sam").orElseThrow().joinCode());
        teams.create("kim", "Grips");
        gaffers = teams.of("sam").orElseThrow().id();
        grips = teams.of("kim").orElseThrow().id();
        staff = new Visitor(mvc).as(SignedIn.staff("ontik"));
    }

    @Test
    void theQueueShowsWhatTeamsAnswered() throws Exception {
        submissions.record(gaffers, "storyboard", "Storyboard", "s1", ANSWERS);
        String page = staff.get(PAGE).getContentAsString();
        assertThat(page).contains("Pending · 1", "Approved · 0", "All · 1", "Gaffers", " · 2 members", "Awaiting review",
                "Prize </span>Stickers", "href=\"https://files.example/board.pdf\"", "Line one", "Line two", "Crew", ">3<",
                ">Approve</button>", ">Send back</button>", "data-confirm=\"Send “Storyboard” back to Gaffers?")
                .doesNotContain("Skipped");
    }

    @Test
    void approvingThenUndoing() throws Exception {
        submissions.record(gaffers, "storyboard", "Storyboard", "s1", "[]");
        var back = staff.post(PAGE + "/review", "teamId", gaffers, "milestoneId", "storyboard", "decision", "APPROVE",
                "filter", "pending", "q", "");
        assertThat(back.getRedirectedUrl()).isEqualTo("/staff/milestones?filter=pending&q=");
        assertThat(submissions.statusesFor("sam")).containsEntry("storyboard", MilestoneStatus.APPROVED);
        assertThat(db.sql("SELECT reviewed_by FROM milestone_submissions").query(String.class).single())
                .isEqualTo("ontik@example.com");

        assertThat(staff.get(PAGE).getContentAsString()).contains("Nothing waiting for review. 🎉");
        assertThat(staff.get(PAGE + "?filter=approved").getContentAsString())
                .contains("Approved ", " by ontik@example.com", ">Undo approval</button>");

        staff.post(PAGE + "/review", "teamId", gaffers, "milestoneId", "storyboard", "decision", "UNDO_APPROVAL");
        assertThat(submissions.statusesFor("sam")).containsEntry("storyboard", MilestoneStatus.SUBMITTED);
        assertThat(db.sql("SELECT reviewed_by IS NULL FROM milestone_submissions").query(Boolean.class).single()).isTrue();
    }

    @Test
    void sendingBackMakesItReadyToSubmitAgain() throws Exception {
        submissions.record(gaffers, "storyboard", "Storyboard", "s1", "[]");
        staff.post(PAGE + "/review", "teamId", gaffers, "milestoneId", "storyboard", "decision", "SEND_BACK");
        assertThat(submissions.statusesFor("sam")).isEmpty();
    }

    @Test
    void aDecisionOnSomethingThatChangedDoesNothing() throws Exception {
        submissions.record(gaffers, "storyboard", "Storyboard", "s1", "[]");
        submissions.review(gaffers, "storyboard", Decision.APPROVE, "someone-else");
        // a second staff member's page still showed it as pending
        assertThat(submissions.review(gaffers, "storyboard", Decision.SEND_BACK, "ontik")).isFalse();

        var page = staff.follow(staff.post(PAGE + "/review", "teamId", gaffers, "milestoneId", "storyboard",
                "decision", "SEND_BACK", "filter", "all", "q", ""));
        assertThat(page.getContentAsString()).contains("That submission changed in the meantime.");
        assertThat(submissions.statusesFor("sam")).containsEntry("storyboard", MilestoneStatus.APPROVED);
    }

    @Test
    void pendingOnesOldestFirstAndSearch() throws Exception {
        submissions.record(gaffers, "storyboard", "Storyboard", "s1", "[]");
        Thread.sleep(5);
        submissions.record(grips, "storyboard", "Storyboard", "s2", "[]");

        String pending = staff.get(PAGE).getContentAsString();
        assertThat(pending.indexOf("<strong>Gaffers</strong>")).isLessThan(pending.indexOf("<strong>Grips</strong>"));
        String all = staff.get(PAGE + "?filter=all").getContentAsString();
        assertThat(all.indexOf("<strong>Grips</strong>")).isLessThan(all.indexOf("<strong>Gaffers</strong>"));

        assertThat(staff.get(PAGE + "?filter=all&q=grip").getContentAsString())
                .contains("<strong>Grips</strong>").doesNotContain("<strong>Gaffers</strong>");
        assertThat(staff.get(PAGE + "?filter=all&q=nothing").getContentAsString()).contains("No submissions match.");
    }

    @Test
    void progressIsATeamByMilestoneTableInTheEventsOrder() throws Exception {
        submissions.record(gaffers, "final", "Final Cut", "s1", "[]");
        submissions.record(gaffers, "storyboard", "Storyboard", "s2", "[]");
        submissions.record(grips, "shoot", "Shoot", "s3", "[]");
        submissions.review(gaffers, "storyboard", Decision.APPROVE, "x");

        String page = staff.get(PAGE + "?view=progress").getContentAsString();
        assertThat(page).contains("2 teams · 1 approved · 2 awaiting review", "Storyboard: Approved", "Final Cut: Awaiting review",
                "Shoot: Not submitted", ">1/3<", ">0/3<", "Prize: Stickers",
                "href=\"/staff/milestones?filter=all&amp;q=Gaffers\"");
        // the event's order (storyboard, shoot, final), not the ids' alphabetical one
        assertThat(page.indexOf("title=\"Storyboard\"")).isLessThan(page.indexOf("title=\"Shoot\""));
        assertThat(page.indexOf("title=\"Shoot\"")).isLessThan(page.indexOf("title=\"Final Cut\""));
        // the most approved team first
        assertThat(page.indexOf(">Gaffers</a>")).isLessThan(page.indexOf(">Grips</a>"));
    }

    @Test
    void forStaffOnly() throws Exception {
        assertThat(new Visitor(mvc).as(SignedIn.participant("sam")).get(PAGE).getRedirectedUrl()).isEqualTo("/participant");
        assertThat(new Visitor(mvc).as(SignedIn.participant("sam"))
                .post(PAGE + "/review", "teamId", gaffers, "milestoneId", "storyboard", "decision", "APPROVE")
                .getRedirectedUrl()).isEqualTo("/participant");
    }
}
