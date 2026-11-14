package org.limelightcreatives.dashboard.event;

/**
 * One milestone of an event (content/events.yml).
 *
 * @param id         what the database stores; never change it once teams have submitted
 * @param targetTime e.g. "Day 1, after workshop 1"
 * @param evidence   what the team needs to show
 * @param bonus      bonus milestones are always open; the others unlock in order
 */
public record Milestone(String id, String label, String description, String targetTime, String evidence,
                        boolean bonus, String prize) {}
