package org.limelightcreatives.dashboard.checkin;

import org.limelightcreatives.dashboard.sheets.Spreadsheet;
import org.limelightcreatives.dashboard.sheets.Spreadsheets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The participant list in the attendee sheet, and who's checked in (app/actions/participants.ts). Same tabs as
 * {@link CheckIns}. Changes go straight to the sheet, so the list is always what the sheet says.
 */
@Service
public class Participants {

    /** Checked in, checked out, or not arrived yet. */
    public enum Status {
        IN("Checked in"), OUT("Checked out"), NONE("Not arrived");

        private final String label;

        Status(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /**
     * Someone in the sheet.
     *
     * @param lastAt when they were last checked in or out, as logged; empty if never
     */
    public record Participant(String name, String email, String phone, String emergency, String registrationId,
                              Status status, String lastAt) {}

    /**
     * The add or edit form. Serializable as it's kept in the session for a moment when saving didn't work.
     *
     * @param originalId the registration ID before editing; empty when adding someone
     */
    public record Input(String originalId, String name, String email, String phone, String emergency,
                        String registrationId) implements Serializable {

        public static final Input EMPTY = new Input("", "", "", "", "", "");

        public Input {
            originalId = s(originalId);
            name = s(name);
            email = s(email);
            phone = s(phone);
            emergency = s(emergency);
            registrationId = s(registrationId);
        }

        public boolean isNew() {
            return originalId.isEmpty();
        }

        static Input of(Participant p) {
            return new Input(p.registrationId(), p.name(), p.email(), p.phone(), p.emergency(), p.registrationId());
        }
    }

    /** Why a change wasn't made, or that the sheet didn't answer. */
    public static class ParticipantException extends RuntimeException {
        ParticipantException(String message) {
            super(message, null, false, false);
        }
    }

    static final String UNREACHABLE = "Couldn't reach the sheet. Try again.";

    private static final Logger log = LoggerFactory.getLogger(Participants.class);

    private final Spreadsheets sheets;
    private final CheckIns checkIns;

    public Participants(Spreadsheets sheets, CheckIns checkIns) {
        this.sheets = sheets;
        this.checkIns = checkIns;
    }

    /** Everyone in the sheet with a name or an ID, with their latest check-in or out. */
    public List<Participant> list() {
        return sheet(sheet -> {
            List<List<String>> people = sheet.values(CheckIns.ATTENDEES + "!A2:E");
            // the latest log row for an ID decides its status, as for a scan
            Map<String, String[]> latest = new HashMap<>();
            for (var row : sheet.values(CheckIns.CHECK_INS + "!A2:D")) {
                String id = s(cell(row, 3)).toLowerCase(Locale.ROOT);
                if (!id.isEmpty()) latest.put(id, new String[]{s(cell(row, 1)), s(cell(row, 2))});
            }
            return people.stream()
                    .filter(r -> !s(cell(r, 4)).isEmpty() || !s(cell(r, 0)).isEmpty())
                    .map(r -> {
                        String id = s(cell(r, 4));
                        String[] last = latest.get(id.toLowerCase(Locale.ROOT));
                        Status status = last == null ? Status.NONE : CheckInResult.CHECK_IN.equals(last[0]) ? Status.IN : Status.OUT;
                        return new Participant(s(cell(r, 0)), s(cell(r, 1)), s(cell(r, 2)), s(cell(r, 3)), id, status,
                                last == null ? "" : last[1]);
                    })
                    .toList();
        });
    }

    /** Adds someone, or changes them (found by {@link Input#originalId()}). */
    public void save(Input input) {
        if (input.name().isEmpty()) throw new ParticipantException("Name is required.");
        if (input.registrationId().isEmpty()) throw new ParticipantException("Registration ID is required.");
        if (input.registrationId().length() > 100) throw new ParticipantException("Registration ID is too long.");

        sheet(sheet -> {
            List<List<String>> people = sheet.values(CheckIns.ATTENDEES + "!A2:E");
            if (!same(input.registrationId(), input.originalId())
                    && people.stream().anyMatch(r -> same(cell(r, 4), input.registrationId()))) {
                throw new ParticipantException("Registration ID \"" + input.registrationId() + "\" is already in use.");
            }
            List<String> values = List.of(input.name(), input.email(), input.phone(), input.emergency(), input.registrationId());
            if (input.isNew()) {
                sheet.append(CheckIns.ATTENDEES + "!A:E", values);
            } else {
                int row = rowOf(people, input.originalId());
                sheet.update(CheckIns.ATTENDEES + "!A" + row + ":E" + row, List.of(values));
            }
            return null;
        });
        this.checkIns.forgetAttendees();
    }

    public void delete(String registrationId) {
        sheet(sheet -> {
            int row = rowOf(sheet.values(CheckIns.ATTENDEES + "!A2:E"), registrationId);
            sheet.deleteRow("Attendees", row);
            return null;
        });
        this.checkIns.forgetAttendees();
    }

    /** The sheet row (1-based, under the header) of whoever has this ID. */
    private static int rowOf(List<List<String>> people, String registrationId) {
        for (int i = 0; i < people.size(); i++) {
            if (same(cell(people.get(i), 4), registrationId)) return i + 2;
        }
        throw new ParticipantException("That participant no longer exists. Refresh the list.");
    }

    private interface SheetWork<T> {
        T run(Spreadsheet sheet);
    }

    private <T> T sheet(SheetWork<T> work) {
        try {
            return work.run(this.sheets.attendees());
        } catch (ParticipantException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("The attendee sheet didn't answer", e);
            throw new ParticipantException(UNREACHABLE);
        }
    }

    private static String s(String value) {
        return value == null ? "" : value.strip();
    }

    private static boolean same(String a, String b) {
        return s(a).equalsIgnoreCase(s(b));
    }

    private static String cell(List<String> row, int index) {
        return CheckIns.cell(row, index);
    }
}
