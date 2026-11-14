package org.limelightcreatives.dashboard.milestone;

/** Where a team is with one milestone. The database only stores SUBMITTED and APPROVED; no row is PENDING. */
public enum MilestoneStatus {

    PENDING("Ready to submit"),
    SUBMITTED("Awaiting review"),
    APPROVED("Approved");

    private final String label;

    MilestoneStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    /** From milestone_submissions.status ('submitted' or 'approved'). */
    static MilestoneStatus of(String stored) {
        return "approved".equals(stored) ? APPROVED : SUBMITTED;
    }
}
