package org.limelightcreatives.dashboard.event;

import java.util.List;

/**
 * A tab of an event's dashboard, in the sidebar (event/shell.html).
 *
 * @param id   also its icon, and what a page passes to the shell to mark it as the current one
 * @param path after {@code /participant/{eventId}}, empty for the dashboard's home
 */
public record Tab(String id, String label, String path) {

    public static final List<Tab> ALL = List.of(
            new Tab("home", "Home", ""),
            new Tab("milestones", "Milestones", "/milestones"),
            new Tab("project", "Project", "/project"),
            new Tab("equipment", "Equipment", "/equipment"),
            new Tab("team", "Team", "/team"),
            new Tab("resources", "Resources", "/resources"));

    public String href(EventConfig event) {
        return "/participant/" + event.id() + path;
    }
}
