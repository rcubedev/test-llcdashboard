package org.limelightcreatives.dashboard.project;

import java.io.Serializable;

/**
 * A team's one project (their film), table submissions. (Serializable as it's kept in the session for a
 * moment when saving didn't work, to show the form again with what they typed.)
 *
 * @param videoUrl where the film is, usually a Google Drive link anyone with the link can view
 */
public record Project(String title, String logline, String videoUrl) implements Serializable {

    static final Project EMPTY = new Project("", "", "");
}
