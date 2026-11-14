package org.limelightcreatives.dashboard.project;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Optional;
import java.util.regex.Pattern;

/** Each team's project, with the old app/api/project rules. */
@Component
public class Projects {

    private static final Pattern WEB_ADDRESS = Pattern.compile("(?i)https?://[^\\s/?#]+\\S*( \\S+)*");
    private static final int TITLE_LENGTH = 120;
    private static final int LOGLINE_LENGTH = 500;

    private final JdbcClient db;
    private final DashboardProperties properties;
    private final Clock clock;

    public Projects(JdbcClient db, DashboardProperties properties, Clock clock) {
        this.db = db;
        this.properties = properties;
        this.clock = clock;
    }

    public Optional<Project> of(String teamId) {
        return this.db.sql("SELECT title, logline, video_url FROM submissions WHERE team_id = ?")
                .param(teamId)
                .query((rs, row) -> new Project(rs.getString(1), rs.getString(2), rs.getString(3)))
                .optional();
    }

    /** Whether SUBMISSIONS_CLOSE_AT has passed. */
    public boolean closed() {
        return this.properties.submissionsClosed(this.clock.instant());
    }

    /**
     * Saves the project of {@code userId}'s team (the team comes from the database, never the form), replacing
     * what was there. Titles are cut to 120 characters and loglines to 500.
     */
    public void save(String userId, String title, String logline, String videoUrl) {
        if (closed()) throw new ProjectException("Submissions are closed");
        String t = cut(title, TITLE_LENGTH);
        String l = cut(logline, LOGLINE_LENGTH);
        String url = videoUrl == null ? "" : videoUrl.strip();
        if (t.isEmpty() || l.isEmpty() || !isWebAddress(url)) {
            throw new ProjectException("Title, logline and a valid video URL are required");
        }

        int saved = this.db.sql("""
                        INSERT INTO submissions (team_id, title, logline, video_url, submitted_by)
                        SELECT team_id, :title, :logline, :url, :user FROM team_members WHERE user_id = :user
                        ON CONFLICT (team_id) DO UPDATE SET
                            title = excluded.title,
                            logline = excluded.logline,
                            video_url = excluded.video_url,
                            submitted_by = excluded.submitted_by,
                            updated_at = now()
                        """)
                .param("title", t).param("logline", l).param("url", url).param("user", userId)
                .update();
        if (saved == 0) throw new ProjectException("Join a team first");
    }

    private static String cut(String value, int length) {
        String s = value == null ? "" : value.strip();
        return s.length() > length ? s.substring(0, length).strip() : s;
    }

    /** An http(s) address with a host, as the old app's {@code new URL(...)} check accepted (spaces in the path included). */
    static boolean isWebAddress(String value) {
        return WEB_ADDRESS.matcher(value).matches();
    }
}
