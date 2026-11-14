package org.limelightcreatives.dashboard.checkin;

import java.io.Serializable;

/**
 * What one scan did (checkin.ts's CheckInResult). Sent as JSON to the camera scanner, and kept in the session for
 * a moment after a typed-in ID so the page can show it.
 *
 * @param status  ok, unknown (no participant with that ID) or error
 * @param action  CHECK IN or CHECK OUT, when ok
 * @param id      the registration ID: as the sheet has it when ok, as scanned when unknown
 * @param at      when, as logged in the sheet (hh:mm:ss dd/mm/yy), when ok
 * @param message what went wrong, on an error
 */
public record CheckInResult(String status, String action, String id, String name, String at, String message)
        implements Serializable {

    public static final String CHECK_IN = "CHECK IN";
    public static final String CHECK_OUT = "CHECK OUT";

    static CheckInResult ok(String action, String id, String name, String at) {
        return new CheckInResult("ok", action, id, name, at, null);
    }

    static CheckInResult unknown(String id) {
        return new CheckInResult("unknown", null, id, null, null, null);
    }

    static CheckInResult error(String message) {
        return new CheckInResult("error", null, null, null, null, message);
    }

    public boolean ok() {
        return "ok".equals(status);
    }

    public boolean checkedIn() {
        return ok() && CHECK_IN.equals(action);
    }

    /** The tag: Checked in, Checked out, Unknown ID or Error. */
    public String label() {
        if (ok()) return checkedIn() ? "Checked in" : "Checked out";
        return "unknown".equals(status) ? "Unknown ID" : "Error";
    }

    /** What went wrong, in a sentence, when it wasn't ok. */
    public String problem() {
        return "unknown".equals(status) ? "No participant with ID \"" + id + "\"." : message;
    }
}
