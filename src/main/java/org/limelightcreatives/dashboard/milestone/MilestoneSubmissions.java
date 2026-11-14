package org.limelightcreatives.dashboard.milestone;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Milestone submissions (table milestone_submissions): recorded from Fillout, reviewed by staff. A milestone
 * without a row is "Ready to submit".
 */
@Component
public class MilestoneSubmissions {

    /** What a staff member can do with a submission. */
    public enum Decision {
        /** Unlocks the team's next milestone. */
        APPROVE,
        /** Deletes it, so the team submits it again. */
        SEND_BACK,
        /** Back to awaiting review. */
        UNDO_APPROVAL
    }

    private final JdbcClient db;
    private final JsonMapper json;

    public MilestoneSubmissions(JdbcClient db, JsonMapper json) {
        this.db = db;
        this.json = json;
    }

    /** The latest 500 submissions, newest first, for staff. */
    public List<Submission> all() {
        return this.db.sql("""
                        SELECT s.team_id::text, t.name,
                               (SELECT count(*) FROM team_members WHERE team_id = s.team_id) AS member_count,
                               s.milestone_id, s.milestone_label, s.status, s.submitted_at, s.reviewed_at,
                               s.reviewed_by, s.submission_id, s.answers::text
                        FROM milestone_submissions s JOIN teams t ON t.id = s.team_id
                        ORDER BY s.submitted_at DESC
                        LIMIT 500
                        """)
                .query((rs, row) -> {
                    String label = rs.getString("milestone_label");
                    return new Submission(rs.getString(1), rs.getString(2), rs.getInt(3), rs.getString("milestone_id"),
                            label == null || label.isEmpty() ? rs.getString("milestone_id") : label,
                            MilestoneStatus.of(rs.getString("status")), instant(rs.getTimestamp("submitted_at")),
                            instant(rs.getTimestamp("reviewed_at")), rs.getString("reviewed_by"),
                            rs.getString("submission_id"), answers(rs.getString("answers")));
                })
                .list();
    }

    /**
     * A staff member's decision on a team's submission. False when the submission had changed in the meantime
     * (someone else reviewed it, or it was sent back): nothing is done then.
     *
     * @param reviewer who decided (their email), kept with an approval
     */
    public boolean review(String teamId, String milestoneId, Decision decision, String reviewer) {
        var statement = switch (decision) {
            case APPROVE -> this.db.sql("""
                            UPDATE milestone_submissions SET status = 'approved', reviewed_at = now(), reviewed_by = ?
                            WHERE team_id = ? AND milestone_id = ? AND status = 'submitted'
                            """)
                    .params(reviewer, teamId, milestoneId);
            case UNDO_APPROVAL -> this.db.sql("""
                            UPDATE milestone_submissions SET status = 'submitted', reviewed_at = NULL, reviewed_by = NULL
                            WHERE team_id = ? AND milestone_id = ? AND status = 'approved'
                            """)
                    .params(teamId, milestoneId);
            // the milestone goes back to "Ready to submit" for the team
            case SEND_BACK -> this.db.sql("""
                            DELETE FROM milestone_submissions
                            WHERE team_id = ? AND milestone_id = ? AND status = 'submitted'
                            """)
                    .params(teamId, milestoneId);
        };
        return statement.update() > 0;
    }

    /** The statuses of someone's team's milestones, by milestone id (none for a milestone not submitted yet). */
    public Map<String, MilestoneStatus> statusesFor(String userId) {
        Map<String, MilestoneStatus> statuses = new HashMap<>();
        this.db.sql("""
                        SELECT s.milestone_id, s.status
                        FROM team_members m JOIN milestone_submissions s ON s.team_id = m.team_id
                        WHERE m.user_id = ?
                        """)
                .param(userId)
                .query(rs -> {
                    statuses.put(rs.getString(1), MilestoneStatus.of(rs.getString(2)));
                });
        return statuses;
    }

    /**
     * A submission from Fillout, awaiting review. Submitting again replaces the last one, but an approved
     * milestone stays approved.
     *
     * @param answers the submission's questions as Fillout sent them, as JSON
     */
    public void record(String teamId, String milestoneId, String milestoneLabel, String submissionId, String answers) {
        this.db.sql("""
                        INSERT INTO milestone_submissions (team_id, milestone_id, status, submission_id, milestone_label, answers)
                        VALUES (?, ?, 'submitted', ?, ?, ?::jsonb)
                        ON CONFLICT (team_id, milestone_id) DO UPDATE SET
                            submission_id = excluded.submission_id,
                            milestone_label = excluded.milestone_label,
                            answers = excluded.answers,
                            submitted_at = now(),
                            status = 'submitted'
                        WHERE milestone_submissions.status <> 'approved'
                        """)
                .params(teamId, milestoneId, submissionId, milestoneLabel, answers)
                .update();
    }

    private static Instant instant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    /** Fillout's questions as readable answers, leaving out the unanswered ones. */
    List<Submission.Answer> answers(String stored) {
        if (stored == null) return List.of();
        JsonNode questions;
        try {
            questions = this.json.readTree(stored);
        } catch (RuntimeException e) {
            return List.of();
        }
        if (!questions.isArray()) return List.of();
        List<Submission.Answer> answers = new ArrayList<>();
        for (JsonNode question : questions) {
            String name = question.path("name").isString() ? question.path("name").asString() : "Answer";
            String value = text(question.path("value"));
            if (!value.isEmpty()) answers.add(new Submission.Answer(name, value));
        }
        return answers;
    }

    /** Whatever Fillout stored for an answer, as text: a file is its address, a list is one line each. */
    private String text(JsonNode value) {
        if (value.isMissingNode() || value.isNull()) return "";
        if (value.isString()) return value.asString().strip();
        if (value.isNumber() || value.isBoolean()) return value.asString();
        if (value.isArray()) {
            List<String> lines = new ArrayList<>();
            for (JsonNode item : value) {
                if (item.isString()) lines.add(item.asString());
                else if (item.path("url").isString()) lines.add(item.path("url").asString());
                else lines.add(this.json.writeValueAsString(item));
            }
            return String.join("\n", lines);
        }
        if (value.path("url").isString()) return value.path("url").asString();
        return this.json.writeValueAsString(value);
    }
}
