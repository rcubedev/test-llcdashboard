package org.limelightcreatives.dashboard;

import org.limelightcreatives.dashboard.sheets.Spreadsheet;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A spreadsheet in memory, for tests and local runs: understands the A1 ranges the dashboard uses
 * ({@code 'Tab'!A2:E}, {@code 'Tab'!A:D}, {@code 'Tab'!C7}, {@code 'Tab'}) and keeps native tables as given.
 */
public class InMemorySpreadsheet implements Spreadsheet {

    private static final Pattern RANGE = Pattern.compile("^'((?:[^']|'')*)'(?:!([A-Z]+)(\\d+)?(?::([A-Z]+)(\\d+)?)?)?$");

    private final Map<String, List<List<String>>> tabs = new LinkedHashMap<>();
    private final Map<String, List<Table>> tables = new LinkedHashMap<>();

    public synchronized InMemorySpreadsheet tab(String name, List<List<String>> rows) {
        List<List<String>> copy = new ArrayList<>();
        rows.forEach(r -> copy.add(new ArrayList<>(r)));
        tabs.put(name, copy);
        tables.putIfAbsent(name, new ArrayList<>());
        return this;
    }

    public synchronized InMemorySpreadsheet table(String tab, Table table) {
        tables.computeIfAbsent(tab, t -> new ArrayList<>()).add(table);
        return this;
    }

    /** Sets one cell (0-based), growing the grid as needed. */
    public synchronized void set(String tab, int row, int column, String value) {
        var grid = tabs.computeIfAbsent(tab, t -> new ArrayList<>());
        while (grid.size() <= row) grid.add(new ArrayList<>());
        var cells = grid.get(row);
        while (cells.size() <= column) cells.add("");
        cells.set(column, value);
    }

    public synchronized String get(String tab, int row, int column) {
        var grid = tabs.getOrDefault(tab, List.of());
        if (row >= grid.size() || column >= grid.get(row).size()) return "";
        return grid.get(row).get(column);
    }

    public synchronized List<List<String>> rows(String tab) {
        return trim(tabs.getOrDefault(tab, List.of()));
    }

    private record Range(String tab, int startRow, int startColumn, int endRow, int endColumn) {}

    private Range parse(String a1) {
        var m = RANGE.matcher(a1);
        if (!m.matches()) throw new IllegalArgumentException("Can't read range " + a1);
        String tab = m.group(1).replace("''", "'");
        if (!tabs.containsKey(tab)) throw new IllegalStateException("Unable to parse range: " + a1);
        if (m.group(2) == null) return new Range(tab, 0, 0, Integer.MAX_VALUE, Integer.MAX_VALUE);
        int startColumn = column(m.group(2));
        int startRow = m.group(3) == null ? 0 : Integer.parseInt(m.group(3)) - 1;
        if (m.group(4) == null) return new Range(tab, startRow, startColumn, m.group(3) == null ? Integer.MAX_VALUE : startRow, startColumn);
        int endColumn = column(m.group(4));
        int endRow = m.group(5) == null ? Integer.MAX_VALUE : Integer.parseInt(m.group(5)) - 1;
        return new Range(tab, startRow, startColumn, endRow, endColumn);
    }

    private static int column(String letters) {
        int n = 0;
        for (char c : letters.toCharArray()) n = n * 26 + (c - 'A' + 1);
        return n - 1;
    }

    @Override
    public synchronized List<List<String>> values(String a1) {
        Range r = parse(a1);
        var grid = tabs.get(r.tab());
        List<List<String>> out = new ArrayList<>();
        for (int row = r.startRow(); row < grid.size() && row <= r.endRow(); row++) {
            List<String> cells = new ArrayList<>();
            var source = grid.get(row);
            for (int c = r.startColumn(); c < source.size() && c <= r.endColumn(); c++) cells.add(source.get(c));
            out.add(cells);
        }
        return trim(out);
    }

    @Override
    public synchronized void append(String a1, List<String> row) {
        Range r = parse(a1);
        int last = trim(tabs.get(r.tab())).size();
        for (int i = 0; i < row.size(); i++) set(r.tab(), last, r.startColumn() + i, row.get(i));
    }

    @Override
    public synchronized void update(String a1, List<List<String>> rows) {
        Range r = parse(a1);
        for (int i = 0; i < rows.size(); i++)
            for (int j = 0; j < rows.get(i).size(); j++) set(r.tab(), r.startRow() + i, r.startColumn() + j, rows.get(i).get(j));
    }

    @Override
    public synchronized void clear(String a1) {
        Range r = parse(a1);
        var grid = tabs.get(r.tab());
        for (int row = r.startRow(); row < grid.size() && row <= r.endRow(); row++) {
            var cells = grid.get(row);
            for (int c = r.startColumn(); c < cells.size() && c <= r.endColumn(); c++) cells.set(c, "");
        }
    }

    @Override
    public synchronized void deleteRow(String tab, int row) {
        tabs.get(tab).remove(row - 1);
    }

    @Override
    public synchronized List<Tab> tabs() {
        List<Tab> out = new ArrayList<>();
        int id = 0;
        for (var name : tabs.keySet()) out.add(new Tab(id++, name, List.copyOf(tables.getOrDefault(name, List.of()))));
        return out;
    }

    /** Like Google: no trailing empty cells or rows. */
    private static List<List<String>> trim(List<List<String>> rows) {
        List<List<String>> out = new ArrayList<>();
        for (var row : rows) {
            int end = row.size();
            while (end > 0 && (row.get(end - 1) == null || row.get(end - 1).isEmpty())) end--;
            out.add(new ArrayList<>(row.subList(0, end)));
        }
        while (!out.isEmpty() && out.getLast().isEmpty()) out.removeLast();
        return out;
    }
}
