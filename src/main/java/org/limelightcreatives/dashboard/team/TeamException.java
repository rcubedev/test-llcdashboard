package org.limelightcreatives.dashboard.team;

/** Why a team change wasn't made, in words for the person who tried (e.g. "That team is full"). */
public class TeamException extends RuntimeException {

    public TeamException(String message) {
        super(message, null, false, false);
    }
}
