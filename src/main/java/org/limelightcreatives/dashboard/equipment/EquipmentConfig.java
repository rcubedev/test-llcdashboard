package org.limelightcreatives.dashboard.equipment;

import org.limelightcreatives.dashboard.Times;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Equipment booking settings (content/equipment.yml): the event's days, when booking opens for each, and
 * what can be booked. Quantities, per-team limits and timeslots come from the Google Sheet.
 *
 * @param jitterSeconds browsers enable RESERVE a random 0..N seconds after a day opens
 * @param slotMinutes   how long each timeslot is
 */
@ConfigurationProperties("equipment")
public record EquipmentConfig(List<Day> days, int jitterSeconds, int slotMinutes, List<Category> categories) {

    private static final DateTimeFormatter OPENS = DateTimeFormatter.ofPattern("h:mm a", Locale.forLanguageTag("en-AU"));

    /**
     * @param opens Sydney time when booking for this day opens; null = already open
     */
    public record Day(LocalDate date, String label, LocalDateTime opens) {}

    /**
     * @param image           a picture under static/, e.g. /equipment/osmo.jpg; optional
     * @param tablePrefix     the sheet's tables are named "&lt;prefix&gt; Day &lt;n&gt;", e.g. "Gimbal Day 1"
     * @param unitName        units are "&lt;unitName&gt; 1", "&lt;unitName&gt; 2", ... (one per slot column)
     * @param quantityLabel   the label next to how many there are, in the sheet's config block
     * @param maxSessionsLabel the label next to how many slots a team may book per day
     */
    public record Category(String id, String label, String description, String image, String tablePrefix,
                           String unitName, String quantityLabel, String maxSessionsLabel) {}

    public EquipmentConfig {
        days = days == null ? List.of() : List.copyOf(days);
        categories = categories == null ? List.of() : List.copyOf(categories);
        if (slotMinutes <= 0) slotMinutes = 60;
    }

    public Optional<Day> day(LocalDate date) {
        return days.stream().filter(d -> d.date().equals(date)).findFirst();
    }

    /** When booking opens for that day, if it has an opening time. */
    public Optional<Instant> opensAt(LocalDate date) {
        return day(date).map(Day::opens).map(t -> t.atZone(Times.SYDNEY).toInstant());
    }

    public boolean hasOpened(LocalDate date, Instant now) {
        return opensAt(date).map(t -> !now.isBefore(t)).orElse(true);
    }

    /** e.g. "10:00 am", Sydney time. */
    public String formatOpensAt(LocalDate date) {
        return day(date).map(Day::opens).map(OPENS::format).orElse("");
    }
}
