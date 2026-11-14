package org.limelightcreatives.dashboard.resource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/** The resources, with the old app/actions/resources.ts rules. */
@Component
public class Resources {

    private static final Pattern WEB_ADDRESS = Pattern.compile("^https?://", Pattern.CASE_INSENSITIVE);
    private static final String COLUMNS =
            "id::text, title, slug, summary, body, url, thumbnail_url, category, kind, audience, published, sort_order, updated_at";

    private static final RowMapper<Resource> ROW = (rs, row) -> new Resource(rs.getString(1), rs.getString(2),
            rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7), rs.getString(8),
            rs.getString(9), rs.getString(10), rs.getBoolean(11), rs.getInt(12), rs.getTimestamp(13).toInstant());

    private final JdbcClient db;
    private final Clock clock;

    public Resources(JdbcClient db, Clock clock) {
        this.db = db;
        this.clock = clock;
    }

    /** What participants see: published, for everyone or for clients, by category then order then title. */
    public List<Resource> forParticipants() {
        return this.db.sql("SELECT " + COLUMNS + """
                         FROM resources
                        WHERE published AND audience IN ('all', 'client')
                        ORDER BY category, sort_order, title
                        """)
                .query(ROW).list();
    }

    /** Everything, drafts too, the latest changed first (for staff). */
    public List<Resource> all() {
        return this.db.sql("SELECT " + COLUMNS + " FROM resources ORDER BY updated_at DESC").query(ROW).list();
    }

    public Optional<Resource> find(String id) {
        try {
            return this.db.sql("SELECT " + COLUMNS + " FROM resources WHERE id = ?").param(id).query(ROW).optional();
        } catch (DataAccessException notAnId) {
            return Optional.empty();
        }
    }

    /** Creates or updates a resource. Returns its id. */
    public String save(ResourceInput input) {
        String title = input.title().strip();
        String category = input.category().isBlank() ? "General" : input.category().strip();
        String url = input.url().strip();
        String thumbnail = input.thumbnailUrl().strip();
        if (title.isEmpty()) throw new ResourceException("Add a title before saving.");
        if (!url.isEmpty() && !WEB_ADDRESS.matcher(url).find()) {
            throw new ResourceException("The link must start with http:// or https://.");
        }
        if (!Resource.KINDS.contains(input.kind()) || !Resource.AUDIENCES.contains(input.audience())) {
            throw new ResourceException("Pick a valid type and audience.");
        }
        if (!thumbnail.isEmpty() && !WEB_ADDRESS.matcher(thumbnail).find()) {
            throw new ResourceException("The thumbnail link must start with http:// or https://.");
        }
        if (input.published() && input.body().isBlank() && url.isEmpty()) {
            throw new ResourceException("Add some content or a link before publishing.");
        }

        if (!input.isNew()) {
            int updated = this.db.sql("""
                            UPDATE resources SET title = ?, summary = ?, body = ?, url = ?, thumbnail_url = ?, category = ?,
                                kind = ?, audience = ?, published = ?, sort_order = ?, updated_at = now()
                            WHERE id = ?
                            """)
                    .params(title, input.summary().strip(), input.body(), url, thumbnail, category, input.kind(),
                            input.audience(), input.published(), input.sortOrder(), input.id())
                    .update();
            if (updated == 0) throw new ResourceException("That resource no longer exists.");
            return input.id();
        }

        String slug = slug(title);
        boolean taken = this.db.sql("SELECT 1 FROM resources WHERE slug = ?").param(slug).query(Integer.class).optional().isPresent();
        if (taken) slug = slug + "-" + suffix();
        return this.db.sql("""
                        INSERT INTO resources (title, slug, summary, body, url, thumbnail_url, category, kind, audience, published, sort_order)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id::text
                        """)
                .params(title, slug, input.summary().strip(), input.body(), url, thumbnail, category, input.kind(),
                        input.audience(), input.published(), input.sortOrder())
                .query(String.class).single();
    }

    public void setPublished(String id, boolean published) {
        try {
            this.db.sql("UPDATE resources SET published = ?, updated_at = now() WHERE id = ?").params(published, id).update();
        } catch (DataAccessException notAnId) {
            // nothing has that id
        }
    }

    public void delete(String id) {
        try {
            this.db.sql("DELETE FROM resources WHERE id = ?").param(id).update();
        } catch (DataAccessException notAnId) {
            // nothing has that id
        }
    }

    /** "How to Light a Scene!" → "how-to-light-a-scene", at most 60 characters. */
    static String slug(String title) {
        // accents come apart from their letters and are dropped: ç → c
        String ascii = Normalizer.normalize(title.toLowerCase(Locale.ROOT), Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
        String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
        if (slug.length() > 60) slug = slug.substring(0, 60);
        return slug.isEmpty() ? "resource" : slug;
    }

    /** The old app's: the last 4 characters of the time in base 36. */
    private String suffix() {
        String time = Long.toString(this.clock.millis(), 36);
        return time.substring(Math.max(0, time.length() - 4));
    }
}
