package org.limelightcreatives.dashboard.event;

import java.util.ArrayList;
import java.util.List;

/**
 * An event with a participant dashboard (content/events.yml).
 *
 * @param id      the key in events.yml; its dashboard is /participant/{id}
 * @param active  false hides it: /participant/{id} says it doesn't exist
 * @param locked  shows "This event isn't open yet" instead of the dashboard
 * @param formUrl the Fillout form for milestone submissions
 */
public record EventConfig(String id, String name, String tagline, boolean active, boolean locked, String formUrl,
                          Theme theme, int teamSize, List<Milestone> milestones) {

    /** Colours for everything inside this event's dashboard (not the top bar). */
    public record Theme(String accent, String accentSecondary) {

        public String style() {
            String secondary = accentSecondary == null || accentSecondary.isBlank() ? accent : accentSecondary;
            return "--accent:" + accent + ";--accent-secondary:" + secondary;
        }
    }

    public EventConfig {
        milestones = milestones == null ? List.of() : List.copyOf(milestones);
        if (theme == null) theme = new Theme("#9fc031", null);
    }

    EventConfig withId(String newId) {
        return new EventConfig(newId, name, tagline, active, locked, formUrl, theme, teamSize, milestones);
    }

    /** The milestones that unlock in order. */
    public List<Milestone> required() {
        return milestones.stream().filter(m -> !m.bonus()).toList();
    }

    public List<Milestone> bonus() {
        return milestones.stream().filter(Milestone::bonus).toList();
    }

    /** Required milestones first, then the bonus ones: the order pages list them in. */
    public List<Milestone> ordered() {
        var all = new ArrayList<>(required());
        all.addAll(bonus());
        return all;
    }
}
