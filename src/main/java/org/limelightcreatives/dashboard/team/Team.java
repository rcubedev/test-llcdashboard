package org.limelightcreatives.dashboard.team;

/**
 * A team, as its members see it.
 *
 * @param id          a UUID, which Fillout passes back with each milestone submission
 * @param joinCode    what teammates enter to join, e.g. "K7QM2X"
 * @param owner       whether the person looking is its owner (who made it, or took over when they left)
 * @param memberCount how many are on it, at most {@link Teams#MAX_SIZE}
 */
public record Team(String id, String name, String joinCode, boolean owner, int memberCount) {

    public int maxSize() {
        return Teams.MAX_SIZE;
    }
}
