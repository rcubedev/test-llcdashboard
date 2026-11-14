package org.limelightcreatives.dashboard.project;

/** Why a project wasn't saved, in words for the person who tried. */
public class ProjectException extends RuntimeException {

    public ProjectException(String message) {
        super(message, null, false, false);
    }
}
