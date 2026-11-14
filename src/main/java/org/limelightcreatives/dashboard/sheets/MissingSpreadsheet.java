package org.limelightcreatives.dashboard.sheets;

import java.util.List;

/** Stands in for a sheet whose settings are missing: everything fails, saying which settings to add. */
record MissingSpreadsheet(String settings) implements Spreadsheet {

    private IllegalStateException missing() {
        return new IllegalStateException("This Google Sheet isn't set up: set " + settings);
    }

    @Override public List<List<String>> values(String range) { throw missing(); }
    @Override public void append(String range, List<String> row) { throw missing(); }
    @Override public void update(String range, List<List<String>> rows) { throw missing(); }
    @Override public void clear(String range) { throw missing(); }
    @Override public void deleteRow(String tab, int row) { throw missing(); }
    @Override public List<Tab> tabs() { throw missing(); }
}
