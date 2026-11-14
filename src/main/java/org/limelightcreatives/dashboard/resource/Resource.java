package org.limelightcreatives.dashboard.resource;

import org.limelightcreatives.dashboard.Times;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A guide, tutorial, video or link on the Resources pages (table resources).
 *
 * @param slug     made from the title when it's created; not used by any page yet
 * @param body     the content: paragraphs separated by a blank line, a line that's only a web address is a link
 * @param url      optional: adds an Open (or Watch) button
 * @param kind     guide, tutorial, video or link
 * @param audience who sees it: all, client (participants) or organiser (staff only)
 * @param sortOrder lower first within a category
 */
public record Resource(String id, String title, String slug, String summary, String body, String url,
                       String thumbnailUrl, String category, String kind, String audience, boolean published,
                       int sortOrder, Instant updatedAt) {

    public static final List<String> KINDS = List.of("guide", "tutorial", "video", "link");
    public static final List<String> AUDIENCES = List.of("all", "client", "organiser");

    private static final Pattern YOUTUBE =
            Pattern.compile("(?:youtube\\.com/(?:watch\\?(?:.*&)?v=|embed/|shorts/)|youtu\\.be/)([A-Za-z0-9_-]{11})",
                    Pattern.CASE_INSENSITIVE);

    public String kindLabel() {
        return kindLabel(kind);
    }

    public static String kindLabel(String kind) {
        return switch (kind) {
            case "tutorial" -> "Tutorial";
            case "video" -> "Video";
            case "link" -> "Link";
            default -> "Guide";
        };
    }

    public String audienceLabel() {
        return audienceLabel(audience);
    }

    public static String audienceLabel(String audience) {
        return switch (audience) {
            case "client" -> "Clients";
            case "organiser" -> "Organisers";
            default -> "Everyone";
        };
    }

    /** The picture on its card: the one set for it, else a YouTube link's own; null when there isn't one. */
    public String thumbnail() {
        return thumbnail(url, thumbnailUrl);
    }

    public static String thumbnail(String url, String thumbnailUrl) {
        if (thumbnailUrl != null && !thumbnailUrl.isBlank()) return thumbnailUrl;
        var youtube = YOUTUBE.matcher(url == null ? "" : url);
        return youtube.find() ? "https://img.youtube.com/vi/" + youtube.group(1) + "/hqdefault.jpg" : null;
    }

    public boolean hasBody() {
        return body != null && !body.isBlank();
    }

    public boolean hasUrl() {
        return url != null && !url.isBlank();
    }

    /** The body's paragraphs, each as its lines. */
    public List<List<String>> paragraphs() {
        if (!hasBody()) return List.of();
        return Arrays.stream(body.split("\\n{2,}"))
                .filter(p -> !p.isBlank())
                .map(p -> List.of(p.split("\n")))
                .toList();
    }

    public String updatedText() {
        return Times.date(updatedAt);
    }

    /** A line is a link when it's a web address. */
    public static boolean isLink(String line) {
        return line.regionMatches(true, 0, "http://", 0, 7) || line.regionMatches(true, 0, "https://", 0, 8);
    }
}
