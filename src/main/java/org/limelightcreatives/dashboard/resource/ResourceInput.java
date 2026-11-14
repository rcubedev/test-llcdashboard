package org.limelightcreatives.dashboard.resource;

import java.io.Serializable;

/**
 * The editor's form (Spring fills it from the post). Serializable as it's kept in the session for a moment when
 * saving didn't work, to show the form again as it was.
 *
 * @param id        empty for a new resource
 * @param published an unticked box isn't sent at all, which is false
 */
public record ResourceInput(String id, String title, String summary, String body, String url, String thumbnailUrl,
                            String category, String kind, String audience, Boolean published, Integer sortOrder)
        implements Serializable {

    public static final ResourceInput EMPTY =
            new ResourceInput(null, "", "", "", "", "", "General", "guide", "all", false, 0);

    public ResourceInput {
        title = orEmpty(title);
        summary = orEmpty(summary);
        body = orEmpty(body);
        url = orEmpty(url);
        thumbnailUrl = orEmpty(thumbnailUrl);
        category = orEmpty(category);
        kind = orEmpty(kind);
        audience = orEmpty(audience);
        if (published == null) published = false;
        if (sortOrder == null) sortOrder = 0;
    }

    static ResourceInput of(Resource r) {
        return new ResourceInput(r.id(), r.title(), r.summary(), r.body(), r.url(), r.thumbnailUrl(), r.category(),
                r.kind(), r.audience(), r.published(), r.sortOrder());
    }

    public boolean isNew() {
        return id == null || id.isBlank();
    }

    /** The thumbnail its card would have, for the editor's preview. */
    public String thumbnail() {
        return Resource.thumbnail(url, thumbnailUrl);
    }

    private static String orEmpty(String s) {
        return s == null ? "" : s;
    }
}
