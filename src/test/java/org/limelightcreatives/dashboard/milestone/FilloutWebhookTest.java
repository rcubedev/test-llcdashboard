package org.limelightcreatives.dashboard.milestone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** What Fillout sends for each milestone submission. */
@AutoConfigureMockMvc
@TestPropertySource(properties = "dashboard.fillout.webhook-secret=the-webhook-secret")
class FilloutWebhookTest extends DatabaseTest {

    @Autowired MockMvc mvc;
    @Autowired Teams teams;
    @Autowired MilestoneSubmissions submissions;

    private String teamId;

    @BeforeEach
    void aTeam() {
        teams.create("sam", "Gaffers");
        teamId = teams.of("sam").orElseThrow().id();
    }

    /** A submission as Fillout sends it (trimmed). */
    private String fillout(String team, String milestone, String answer) {
        return """
                {"formId": "qyMeFhFzZuus", "formName": "Milestones",
                 "submission": {
                   "submissionId": "sub-1", "submissionTime": "2026-10-24T01:00:00.000Z",
                   "questions": [
                     {"id": "q1", "name": "Storyboard", "type": "FileUpload", "value": [{"url": "https://files.example/board.pdf", "filename": "board.pdf"}]},
                     {"id": "q2", "name": "Notes", "type": "LongAnswer", "value": "%s"}
                   ],
                   "urlParameters": [
                     {"id": "a", "name": "milestoneId", "value": "%s"},
                     {"id": "b", "name": "milestone", "value": "Storyboard"},
                     {"id": "c", "name": "teamId", "value": "%s"},
                     {"id": "d", "name": "team", "value": "Gaffers"}
                   ]
                 }}
                """.formatted(answer, milestone, team);
    }

    private MockHttpServletResponse send(String secret, String body) throws Exception {
        var request = post("/api/fillout/webhook").contentType(MediaType.APPLICATION_JSON).content(body);
        if (secret != null) request.param("secret", secret);
        return mvc.perform(request).andReturn().getResponse();
    }

    @Test
    void recordsTheSubmissionForReview() throws Exception {
        var response = send("the-webhook-secret", fillout(teamId, "storyboard", "First go"));
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).isEqualTo("{\"ok\":true}");

        assertThat(submissions.statusesFor("sam")).containsExactlyEntriesOf(java.util.Map.of("storyboard", MilestoneStatus.SUBMITTED));
        var row = db.sql("SELECT milestone_label, submission_id, answers::text FROM milestone_submissions")
                .query((rs, n) -> new String[]{rs.getString(1), rs.getString(2), rs.getString(3)}).single();
        assertThat(row[0]).isEqualTo("Storyboard");
        assertThat(row[1]).isEqualTo("sub-1");
        assertThat(row[2]).contains("\"name\": \"Notes\"", "First go", "board.pdf");
    }

    @Test
    void resubmittingReplacesItButNeverUndoesAnApproval() throws Exception {
        send("the-webhook-secret", fillout(teamId, "storyboard", "First go"));
        send("the-webhook-secret", fillout(teamId, "storyboard", "Second go"));
        assertThat(db.sql("SELECT answers::text FROM milestone_submissions").query(String.class).single()).contains("Second go");

        db.sql("UPDATE milestone_submissions SET status = 'approved'").update();
        send("the-webhook-secret", fillout(teamId, "storyboard", "Third go"));
        assertThat(submissions.statusesFor("sam")).containsEntry("storyboard", MilestoneStatus.APPROVED);
        assertThat(db.sql("SELECT answers::text FROM milestone_submissions").query(String.class).single()).contains("Second go");
    }

    @Test
    void onlyWithTheSecret() throws Exception {
        assertThat(send(null, fillout(teamId, "storyboard", "x")).getStatus()).isEqualTo(401);
        assertThat(send("wrong", fillout(teamId, "storyboard", "x")).getStatus()).isEqualTo(401);
        assertThat(db.sql("SELECT count(*) FROM milestone_submissions").query(Integer.class).single()).isZero();
    }

    @Test
    void badSubmissionsAreRefused() throws Exception {
        assertThat(send("the-webhook-secret", "not json").getStatus()).isEqualTo(400);
        assertThat(send("the-webhook-secret", "{\"submission\": {\"urlParameters\": []}}").getContentAsString())
                .contains("Missing milestoneId or teamId");
        // a team that isn't there, and an id that isn't a UUID at all
        assertThat(send("the-webhook-secret", fillout("00000000-0000-0000-0000-000000000000", "storyboard", "x"))
                .getContentAsString()).contains("Could not record submission");
        assertThat(send("the-webhook-secret", fillout("team-1", "storyboard", "x")).getStatus()).isEqualTo(400);
    }
}
