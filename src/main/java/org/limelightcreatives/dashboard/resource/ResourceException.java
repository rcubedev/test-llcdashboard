package org.limelightcreatives.dashboard.resource;

/** Why a resource wasn't saved, in words for the organiser who tried. */
public class ResourceException extends RuntimeException {

    public ResourceException(String message) {
        super(message, null, false, false);
    }
}
