package org.limelightcreatives.dashboard.sheets;

import java.util.List;

/**
 * The parts of the Google Sheets API the dashboard uses, on one spreadsheet. Ranges are in A1 notation,
 * e.g. {@code 'Attendees'!A2:E}. Values are what the sheet shows (formatted), as text.
 */
public interface Spreadsheet {

    /** The cells in a range, row by row; trailing empty cells and rows are left off, as Google does. */
    List<List<String>> values(String range);

    /** Adds a row after the last one in the range's table. Values are stored as typed (never run as formulas). */
    void append(String range, List<String> row);

    /** Overwrites the cells in a range, starting at its top left. Values are stored as typed. */
    void update(String range, List<List<String>> rows);

    /** Empties the cells in a range. */
    void clear(String range);

    /** Deletes a row from a tab, moving the rows below it up. {@code row} is 1-based (the header is row 1). */
    void deleteRow(String tab, int row);

    /** Every tab with its native tables (Insert > Table in Google Sheets). */
    List<Tab> tabs();

    record Tab(int sheetId, String title, List<Table> tables) {}

    /** A native table's cells: rows and columns are 0-based, with the end exclusive. */
    record Table(String name, int startRow, int endRow, int startColumn, int endColumn) {}

    /** {@code 'Tab name'}, quoted for A1 notation. */
    static String quote(String tab) {
        return "'" + tab.replace("'", "''") + "'";
    }

    /** 0 → A, 25 → Z, 26 → AA. */
    static String column(int index) {
        var letters = new StringBuilder();
        for (int c = index + 1; c > 0; c = (c - 1) / 26) letters.insert(0, (char) ('A' + (c - 1) % 26));
        return letters.toString();
    }
}
