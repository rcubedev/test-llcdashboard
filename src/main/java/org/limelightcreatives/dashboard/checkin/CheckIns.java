package org.limelightcreatives.dashboard.checkin;

import org.limelightcreatives.dashboard.Times;
import org.limelightcreatives.dashboard.sheets.Spreadsheet;
import org.limelightcreatives.dashboard.sheets.Spreadsheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checking participants in and out by registration ID (the QR code on their ticket), logged in the attendee
 * sheet (app/actions/checkin.ts). Each scan flips the person: in, out, in again.
 *
 * <ul>
 *     <li>tab "Attendees": A Name, B Email, C Phone No., D Emergency No., E Registration ID</li>
 *     <li>tab "Check-ins": A Participant Name, B Action, C Timestamp, D Registration ID</li>
 * </ul>
 */
@Service
public class CheckIns {

    public static final String ATTENDEES = Spreadsheet.quote("Attendees");
    public static final String CHECK_INS = Spreadsheet.quote("Check-ins");

    /** Someone just added to the sheet can take this long to be recognised (the Participants page clears it). */
    private static final Duration ATTENDEE_CACHE = Duration.ofMinutes(1);
    /** 11:30:05 24/10/26, in Sydney (servers usually run in UTC) */
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yy", Locale.ROOT).withZone(Times.SYDNEY);

    private static final Logger log = LoggerFactory.getLogger(CheckIns.class);

    private final Spreadsheets sheets;
    private final Clock clock;
    /** IDs being checked right now, so a code held in front of the camera isn't logged twice. */
    private final Set<String> inFlight = ConcurrentHashMap.newKeySet();

    private record Cached(Spreadsheet sheet, List<List<String>> rows, Instant at) {}

    private volatile Cached attendees;

    public CheckIns(Spreadsheets sheets, Clock clock) {
        this.sheets = sheets;
        this.clock = clock;
    }

    /** Checks in (or out) whoever has this registration ID. */
    public CheckInResult scan(String raw) {
        String id = raw == null ? "" : raw.strip();
        if (id.isEmpty() || id.length() > 100) return CheckInResult.error("Invalid code.");
        String key = id.toLowerCase(Locale.ROOT);

        if (!this.inFlight.add(key)) return CheckInResult.error("Scan already processing.");
        try {
            Spreadsheet sheet = this.sheets.attendees();
            List<String> person = attendees(sheet).stream()
                    .filter(row -> cell(row, 4).strip().toLowerCase(Locale.ROOT).equals(key))
                    .findFirst().orElse(null);
            if (person == null) return CheckInResult.unknown(id);

            String name = cell(person, 0);
            String registrationId = cell(person, 4).strip();

            // their latest row in the log decides what this scan does
            String last = null;
            for (var row : sheet.values(CHECK_INS + "!A2:D")) {
                if (cell(row, 3).strip().toLowerCase(Locale.ROOT).equals(key)) last = cell(row, 1);
            }
            String action = CheckInResult.CHECK_IN.equals(last) ? CheckInResult.CHECK_OUT : CheckInResult.CHECK_IN;

            String at = STAMP.format(this.clock.instant());
            sheet.append(CHECK_INS + "!A:D", List.of(name, action, at, registrationId));
            return CheckInResult.ok(action, registrationId, name, at);
        } catch (RuntimeException e) {
            log.error("Check-in failed", e);
            return CheckInResult.error("Couldn't reach the sheet. Try again.");
        } finally {
            this.inFlight.remove(key);
        }
    }

    /** Forgets the attendees read a moment ago, e.g. after someone is added on the Participants page. */
    public void forgetAttendees() {
        this.attendees = null;
    }

    private List<List<String>> attendees(Spreadsheet sheet) {
        Instant now = this.clock.instant();
        Cached cached = this.attendees;
        if (cached != null && cached.sheet() == sheet && now.isBefore(cached.at().plus(ATTENDEE_CACHE))) return cached.rows();
        var rows = sheet.values(ATTENDEES + "!A2:E");
        this.attendees = new Cached(sheet, rows, now);
        return rows;
    }

    static String cell(List<String> row, int index) {
        return index < row.size() && row.get(index) != null ? row.get(index) : "";
    }
}
