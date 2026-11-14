package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.event.Milestone;
import org.limelightcreatives.dashboard.team.Team;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A team's milestones for an event, as the Home and Milestones tabs show them. Required milestones unlock in
 * order (each once the one before is approved); bonus ones are always open.
 */
public class MilestoneProgress {

    /**
     * One milestone, with where the team is with it.
     *
     * @param position  1, 2, 3... in the order the pages list them (required first, then bonus)
     * @param locked    the milestone before it isn't approved yet
     * @param previous  that milestone, when this one is locked
     * @param submitUrl the Fillout form, with the milestone and team added for the webhook
     */
    public record Entry(Milestone milestone, MilestoneStatus status, int position, boolean locked,
                        Milestone previous, String submitUrl) {

        /** What the tag says. */
        public String tag() {
            return locked ? "Locked" : status.label();
        }

        public boolean approved() {
            return status == MilestoneStatus.APPROVED && !locked;
        }

        public boolean canSubmit() {
            return !locked && status == MilestoneStatus.PENDING;
        }

        /** "01", or ★ for a bonus milestone. */
        public String number() {
            return milestone.bonus() ? "★" : "%02d".formatted(position);
        }

        /** On the roadmap: ✓ once approved, otherwise the number. */
        public String badge() {
            return approved() ? "✓" : number();
        }
    }

    private final List<Entry> entries;

    public MilestoneProgress(EventConfig event, Team team, Map<String, MilestoneStatus> statuses) {
        this.entries = new ArrayList<>();
        Entry previous = null;
        for (Milestone m : event.ordered()) {
            var status = statuses.getOrDefault(m.id(), MilestoneStatus.PENDING);
            boolean locked = !m.bonus() && previous != null && previous.status() != MilestoneStatus.APPROVED;
            var entry = new Entry(m, status, this.entries.size() + 1, locked, locked ? previous.milestone() : null,
                    submitUrl(event, m, team));
            this.entries.add(entry);
            if (!m.bonus()) previous = entry;
        }
    }

    /** Required first, then the bonus ones. */
    public List<Entry> entries() {
        return entries;
    }

    public List<Entry> required() {
        return entries.stream().filter(e -> !e.milestone().bonus()).toList();
    }

    public List<Entry> bonus() {
        return entries.stream().filter(e -> e.milestone().bonus()).toList();
    }

    public long approvedCount() {
        return required().stream().filter(e -> e.status() == MilestoneStatus.APPROVED).count();
    }

    public long bonusApproved() {
        return bonus().stream().filter(e -> e.status() == MilestoneStatus.APPROVED).count();
    }

    /** Every submitted milestone waiting on an organiser, bonus ones too. */
    public List<Entry> awaiting() {
        return entries.stream().filter(e -> e.status() == MilestoneStatus.SUBMITTED).toList();
    }

    /** The first required milestone not approved yet: what the team works on now. Null once they're all done. */
    public Entry current() {
        return required().stream().filter(e -> e.status() != MilestoneStatus.APPROVED).findFirst().orElse(null);
    }

    public boolean allDone() {
        return current() == null;
    }

    /** How far through the required milestones, 0 to 100. */
    public int percent() {
        int total = required().size();
        return total == 0 ? 0 : Math.round(100f * approvedCount() / total);
    }

    /** The URL parameters Fillout passes back to the webhook; the labels are just for people reading Fillout. */
    static String submitUrl(EventConfig event, Milestone milestone, Team team) {
        if (team == null || event.formUrl() == null || event.formUrl().isBlank()) return null;
        return UriComponentsBuilder.fromUriString(event.formUrl())
                .queryParam("milestoneId", "{milestoneId}")
                .queryParam("milestone", "{milestone}")
                .queryParam("teamId", "{teamId}")
                .queryParam("team", "{team}")
                .encode()
                .buildAndExpand(Map.of("milestoneId", milestone.id(), "milestone", milestone.label(),
                        "teamId", team.id(), "team", team.name()))
                .toUriString();
    }
}
