package org.limelightcreatives.dashboard.equipment;

import org.limelightcreatives.dashboard.Times;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

/** Times of day as the sheet and the page use them: 24-hour "HH:mm" strings. */
public final class Slots {

    private static final Pattern TIME = Pattern.compile("^(\\d{1,2})(?::(\\d{2}))?(?::\\d{2})?\\s*([ap])?\\.?m?\\.?$", Pattern.CASE_INSENSITIVE);

    private Slots() {}

    /** A booking slot; its id is its start time. */
    public record Slot(String id, String start, String end) {

        /** "1:00 PM – 2:00 PM" */
        public String label() {
            return format(start) + " – " + format(end);
        }
    }

    /** "10:00 AM", "1:00 pm", "13:00" or "13:00:00" → "13:00"; null if it isn't a time. */
    public static String parse(String raw) {
        var m = TIME.matcher(raw.strip());
        if (!m.matches()) return null;
        int h = Integer.parseInt(m.group(1));
        int min = m.group(2) == null ? 0 : Integer.parseInt(m.group(2));
        String meridiem = m.group(3);
        if (meridiem != null) {
            if (h < 1 || h > 12) return null;
            h = h % 12 + (meridiem.equalsIgnoreCase("p") ? 12 : 0);
        }
        if (h > 23 || min > 59) return null;
        return String.format("%02d:%02d", h, min);
    }

    /** Adds minutes, stopping at 23:59. */
    public static String addMinutes(String time, int minutes) {
        int total = Math.min(toMinutes(time) + minutes, 23 * 60 + 59);
        return String.format("%02d:%02d", total / 60, total % 60);
    }

    /** "13:00" → "1:00 PM" */
    public static String format(String time) {
        int total = toMinutes(time);
        int h = total / 60, m = total % 60;
        return (h % 12 == 0 ? 12 : h % 12) + ":" + String.format("%02d", m) + " " + (h < 12 ? "AM" : "PM");
    }

    public static boolean hasEnded(LocalDate date, Slot slot, Instant now) {
        return now.isAfter(date.atTime(LocalTime.parse(slot.end())).atZone(Times.SYDNEY).toInstant());
    }

    static int toMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }
}
