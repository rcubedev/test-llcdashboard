package org.limelightcreatives.dashboard.web;

import java.util.List;

/**
 * A tile on a launchpad (the old lib/apps.tsx).
 *
 * @param icon     a path under static/, e.g. /apps/schedule.svg
 * @param external opens in a new tab
 */
public record App(String name, String href, String icon, boolean external) {

    public static final List<App> PARTICIPANT = List.of(
            new App("Limelight GENESIS", "/participant/genesis", "/apps/genesis.svg", false),
            new App("Schedule", "/participant/schedule", "/apps/schedule.svg", false));

    public static final List<App> STAFF = List.of(
            new App("Schedule", "/staff/schedule", "/apps/schedule.svg", false),
            new App("Check-in", "/staff/checkin", "/apps/onboard.svg", false),
            new App("Milestones", "/staff/milestones", "/apps/milestones.svg", false),
            new App("Participants", "/staff/participants", "/apps/profile.svg", false),
            new App("Resources", "/staff/resources", "/apps/resources.svg", false));
}
