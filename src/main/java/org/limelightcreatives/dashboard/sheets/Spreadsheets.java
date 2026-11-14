package org.limelightcreatives.dashboard.sheets;

/** The dashboard's two Google Sheets. */
public interface Spreadsheets {

    /** Participants (tab "Attendees") and the check-in log (tab "Check-ins"). */
    Spreadsheet attendees();

    /** The equipment booking grid. */
    Spreadsheet equipment();
}
