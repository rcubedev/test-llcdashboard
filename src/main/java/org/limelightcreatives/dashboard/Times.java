package org.limelightcreatives.dashboard;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Times as the pages show them: in Sydney, where the events are, however the server's clock is set. (The old
 * app formatted them in each visitor's browser instead.)
 */
public final class Times {

    public static final ZoneId SYDNEY = ZoneId.of("Australia/Sydney");
    public static final Locale AUSTRALIA = Locale.forLanguageTag("en-AU");

    /** 24/10/2026, 10:31 am */
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("d/MM/yyyy, h:mm a", AUSTRALIA);
    /** 24/10/2026 */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d/MM/yyyy", AUSTRALIA);
    /** 10:31 am */
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", AUSTRALIA);

    private Times() {}

    public static String dateTime(Instant instant) {
        return instant == null ? "" : DATE_TIME.format(instant.atZone(SYDNEY));
    }

    public static String date(Instant instant) {
        return instant == null ? "" : DATE.format(instant.atZone(SYDNEY));
    }

    public static String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant.atZone(SYDNEY));
    }
}
