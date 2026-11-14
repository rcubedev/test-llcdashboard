package org.limelightcreatives.dashboard.team;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.SecureRandom;
import java.util.Optional;

/**
 * Teams and who is on them (tables teams and team_members), with the old app's rules (app/api/team): one team
 * per person, at most {@link #MAX_SIZE} on a team, and nothing changes while teams are locked (TEAMS_LOCKED).
 *
 * <p>Teams aren't per event: someone keeps their team across events, as in the old app.
 */
@Component
public class Teams {

    public static final int MAX_SIZE = 3;

    /** Join codes leave out 0/O, 1/I and L, which are easy to mix up. */
    private static final String ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final int NAME_LENGTH = 60;

    private final JdbcClient db;
    private final TransactionTemplate transaction;
    private final DashboardProperties properties;
    private final SecureRandom random = new SecureRandom();

    public Teams(JdbcClient db, TransactionTemplate transaction, DashboardProperties properties) {
        this.db = db;
        this.transaction = transaction;
        this.properties = properties;
    }

    /** Someone's team, if they're on one. */
    public Optional<Team> of(String userId) {
        return this.db.sql("""
                        SELECT t.id::text, t.name, t.join_code, t.owner_id = :user AS owner,
                               (SELECT count(*) FROM team_members WHERE team_id = t.id) AS member_count
                        FROM team_members m JOIN teams t ON t.id = m.team_id
                        WHERE m.user_id = :user
                        """)
                .param("user", userId)
                .query((rs, row) -> new Team(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getInt(5)))
                .optional();
    }

    public boolean locked() {
        return this.properties.teamsLocked();
    }

    /** Makes a team with this person as its owner and only member. */
    public void create(String userId, String name) {
        String teamName = name == null ? "" : name.strip();
        if (teamName.length() > NAME_LENGTH) teamName = teamName.substring(0, NAME_LENGTH);
        checkUnlocked();
        if (teamName.isEmpty()) throw new TeamException("Team name required");

        String finalName = teamName;
        inTransaction(() -> {
            checkNotOnATeam(userId);
            String teamId = null;
            // a code that's already taken just means trying another
            for (int i = 0; i < 5 && teamId == null; i++) {
                teamId = this.db.sql("""
                                INSERT INTO teams (name, join_code, owner_id) VALUES (?, ?, ?)
                                ON CONFLICT (join_code) DO NOTHING RETURNING id::text
                                """)
                        .params(finalName, joinCode(), userId)
                        .query(String.class).optional().orElse(null);
            }
            if (teamId == null) throw new IllegalStateException("Couldn't find an unused join code");
            addMember(userId, teamId);
        });
    }

    /** Joins the team with this code, if it has room. */
    public void join(String userId, String code) {
        String joinCode = code == null ? "" : code.strip().toUpperCase();
        checkUnlocked();
        if (joinCode.isEmpty()) throw new TeamException("Join code required");

        inTransaction(() -> {
            checkNotOnATeam(userId);
            // FOR UPDATE locks the team's row, so two people can't both take its last place
            String teamId = this.db.sql("SELECT id::text FROM teams WHERE join_code = ? FOR UPDATE")
                    .param(joinCode).query(String.class).optional()
                    .orElseThrow(() -> new TeamException("No team with that code"));
            int members = this.db.sql("SELECT count(*) FROM team_members WHERE team_id = ?")
                    .param(teamId).query(Integer.class).single();
            if (members >= MAX_SIZE) throw new TeamException("That team is full");
            addMember(userId, teamId);
        });
    }

    /**
     * Leaves their team. The longest-standing member left takes over if the owner leaves; a team with nobody
     * left is deleted, with its project and milestone submissions.
     */
    public void leave(String userId) {
        checkUnlocked();
        inTransaction(() -> {
            String teamId = this.db.sql("DELETE FROM team_members WHERE user_id = ? RETURNING team_id::text")
                    .param(userId).query(String.class).optional()
                    .orElseThrow(() -> new TeamException("You're not on a team"));
            Optional<String> next = this.db.sql(
                            "SELECT user_id FROM team_members WHERE team_id = ? ORDER BY joined_at LIMIT 1")
                    .param(teamId).query(String.class).optional();
            if (next.isEmpty()) {
                this.db.sql("DELETE FROM teams WHERE id = ?").param(teamId).update();
            } else {
                this.db.sql("UPDATE teams SET owner_id = ? WHERE id = ? AND owner_id = ?")
                        .params(next.get(), teamId, userId).update();
            }
        });
    }

    private void checkUnlocked() {
        if (locked()) throw new TeamException("Teams are locked");
    }

    private void checkNotOnATeam(String userId) {
        boolean onATeam = this.db.sql("SELECT 1 FROM team_members WHERE user_id = ?")
                .param(userId).query(Integer.class).optional().isPresent();
        if (onATeam) throw new TeamException("You're already on a team");
    }

    private void addMember(String userId, String teamId) {
        try {
            this.db.sql("INSERT INTO team_members (user_id, team_id) VALUES (?, ?)").params(userId, teamId).update();
        } catch (DuplicateKeyException e) {
            // they joined another team at the same moment (team_members.user_id is the key)
            throw new TeamException("You're already on a team");
        }
    }

    private void inTransaction(Runnable work) {
        this.transaction.executeWithoutResult(status -> work.run());
    }

    private String joinCode() {
        var code = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) code.append(ALPHABET.charAt(this.random.nextInt(ALPHABET.length())));
        return code.toString();
    }
}
