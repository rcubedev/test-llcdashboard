package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.Times;

import java.time.Instant;
import java.util.List;

/**
 * A milestone submission, as staff review it.
 *
 * @param milestoneLabel the label Fillout passed back, or the id when there wasn't one
 * @param reviewedBy     who approved it (their email)
 * @param answers        what the team answered in Fillout
 */
public record Submission(String teamId, String teamName, int memberCount, String milestoneId, String milestoneLabel,
                         MilestoneStatus status, Instant submittedAt, Instant reviewedAt, String reviewedBy,
                         String submissionId, List<Answer> answers) {

    /**
     * One answer in a submission, as text: a file upload becomes its address, a list one line each.
     *
     * @param name the question
     */
    public record Answer(String name, String value) {

        public List<String> lines() {
            return List.of(value.split("\n", -1));
        }
    }

    public boolean approved() {
        return status == MilestoneStatus.APPROVED;
    }

    /** What pages and forms use to say which submission: a team has at most one per milestone. */
    public String key() {
        return teamId + ":" + milestoneId;
    }

    public String submittedAtText() {
        return Times.dateTime(submittedAt);
    }

    public String reviewedAtText() {
        return Times.dateTime(reviewedAt);
    }

    /** A line is a link when it's a web address. */
    public static boolean isLink(String line) {
        return line.regionMatches(true, 0, "http://", 0, 7) || line.regionMatches(true, 0, "https://", 0, 8);
    }
}
