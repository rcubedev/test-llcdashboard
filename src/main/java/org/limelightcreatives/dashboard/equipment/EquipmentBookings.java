package org.limelightcreatives.dashboard.equipment;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.limelightcreatives.dashboard.equipment.EquipmentSnapshot.Booking;
import org.limelightcreatives.dashboard.equipment.EquipmentSnapshot.CategoryState;
import org.limelightcreatives.dashboard.sheets.Spreadsheet;
import org.limelightcreatives.dashboard.sheets.Spreadsheets;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

/**
 * Reads and writes the equipment booking grid in Google Sheets (layout described in content/equipment.yml).
 * The sheet is the source of truth: every booking reads it fresh, writes one cell, and reads it back to
 * make sure nobody else got the same cell first.
 */
@Service
public class EquipmentBookings {

    /** The table layout changes rarely, so it's kept for a minute. */
    private static final Duration META_CACHE = Duration.ofMinutes(1);
    /** A short cache so a room full of people refreshing doesn't use up the Sheets read quota. */
    private static final Duration SNAPSHOT_CACHE = Duration.ofSeconds(5);
    private static final Pattern TABLE_NAME = Pattern.compile("^(.*?)\\s+day\\s+(\\d+)$", Pattern.CASE_INSENSITIVE);

    private final Spreadsheets sheets;
    private final EquipmentConfig config;
    private final String wantedTab;
    private final Clock clock;
    /** One write at a time on this instance, so two requests can't both see the same free unit. */
    private final ReentrantLock writing = new ReentrantLock();

    /** The tab and its tables, as read from {@code sheet} at {@code at}. */
    private record Meta(Spreadsheet sheet, String sheetTitle, List<Spreadsheet.Table> tables, Instant at) {}

    /** The bookings, as read from {@code sheet} at {@code at}. */
    private record Snapshot(Spreadsheet sheet, EquipmentSnapshot snapshot, Instant at) {}

    private volatile Meta meta;
    private volatile Snapshot snapshot;

    public EquipmentBookings(Spreadsheets sheets, EquipmentConfig config, DashboardProperties properties, Clock clock) {
        this.sheets = sheets;
        this.config = config;
        this.wantedTab = properties.equipmentSheet().tab();
        this.clock = clock;
    }

    /** What the sheet says, at most a few seconds old. */
    public EquipmentSnapshot snapshot() {
        var cached = snapshot;
        if (cached != null && cached.sheet() == sheets.equipment() && clock.instant().isBefore(cached.at().plus(SNAPSHOT_CACHE))) {
            return cached.snapshot();
        }
        return remember(read().snapshot());
    }

    /** Books the first free unit of a category in a slot for the team. */
    public EquipmentSnapshot reserve(String team, String categoryId, LocalDate date, String slotId) {
        // Checked first so early clicks cost no Sheets quota.
        if (!config.hasOpened(date, clock.instant())) {
            throw new BookingException("Bookings for this day open " + config.formatOpensAt(date));
        }
        writing.lock();
        try {
            State state = read(); // always fresh, never cached
            Target target = target(state.snapshot(), categoryId, date, slotId);

            var mine = target.here().stream().filter(b -> EquipmentSnapshot.sameTeam(b.team(), team)).toList();
            if (mine.stream().anyMatch(b -> b.slotId().equals(slotId))) throw new BookingException("Your team already has this slot");
            if (mine.size() >= target.category().maxSessions()) {
                throw new BookingException("Your team has reached the limit of " + target.category().maxSessions()
                        + " for this equipment today");
            }

            var taken = new HashSet<String>();
            target.here().stream().filter(b -> b.slotId().equals(slotId)).forEach(b -> taken.add(b.unit()));
            List<String> units = target.category().units();
            int unitIndex = -1;
            for (int i = 0; i < units.size() && unitIndex < 0; i++) if (!taken.contains(units.get(i))) unitIndex = i;
            if (unitIndex < 0) throw new BookingException("Someone just booked the last one in that slot");

            Cell cell = state.locate(categoryId, date.toString(), slotId, unitIndex)
                    .orElseThrow(() -> new BookingException("That slot moved in the sheet. Refresh and try again"));
            sheets.equipment().update(cell.a1(state.sheetTitle()), List.of(List.of(team)));

            // Read back: if another server instance wrote the same cell after us, we'll see their name.
            State after = read();
            String unit = units.get(unitIndex);
            boolean ours = after.snapshot().bookings().stream().anyMatch(b -> b.categoryId().equals(categoryId)
                    && b.date().equals(date.toString()) && b.slotId().equals(slotId) && b.unit().equals(unit)
                    && EquipmentSnapshot.sameTeam(b.team(), team));
            remember(after.snapshot());
            if (!ours) throw new BookingException("Someone else got that slot first. Try another");
            return after.snapshot();
        } finally {
            writing.unlock();
        }
    }

    /** Cancels the team's booking in a slot. */
    public EquipmentSnapshot cancel(String team, String categoryId, LocalDate date, String slotId) {
        writing.lock();
        try {
            State state = read();
            Target target = target(state.snapshot(), categoryId, date, slotId);
            Booking booking = target.here().stream()
                    .filter(b -> b.slotId().equals(slotId) && EquipmentSnapshot.sameTeam(b.team(), team))
                    .findFirst()
                    .orElseThrow(() -> new BookingException("Your team doesn't have this slot"));

            Cell cell = state.locate(categoryId, date.toString(), slotId, target.category().units().indexOf(booking.unit()))
                    .orElseThrow(() -> new BookingException("That slot moved in the sheet. Refresh and try again"));
            sheets.equipment().clear(cell.a1(state.sheetTitle()));

            State after = read();
            return remember(after.snapshot());
        } finally {
            writing.unlock();
        }
    }

    /** Forgets what was read from the sheet, so the next read goes to Google (e.g. after editing the sheet). */
    public void forgetCached() {
        snapshot = null;
        meta = null;
    }

    private EquipmentSnapshot remember(EquipmentSnapshot fresh) {
        snapshot = new Snapshot(sheets.equipment(), fresh, clock.instant());
        return fresh;
    }

    private record Target(CategoryState category, Slots.Slot slot, List<Booking> here) {}

    private Target target(EquipmentSnapshot snapshot, String categoryId, LocalDate date, String slotId) {
        CategoryState category = snapshot.categories().get(categoryId);
        if (category == null) throw new BookingException("Unknown equipment category");
        Slots.Slot slot = category.days().getOrDefault(date.toString(), List.of()).stream()
                .filter(s -> s.id().equals(slotId)).findFirst()
                .orElseThrow(() -> new BookingException("That slot doesn't exist"));
        if (Slots.hasEnded(date, slot, clock.instant())) throw new BookingException("That slot has already finished");
        var here = snapshot.bookings().stream()
                .filter(b -> b.categoryId().equals(categoryId) && b.date().equals(date.toString()))
                .toList();
        return new Target(category, slot, here);
    }

    // ---------- Reading ----------

    /** A cell, 0-based. */
    private record Cell(int row, int column) {

        String a1(String sheetTitle) {
            return Spreadsheet.quote(sheetTitle) + "!" + Spreadsheet.column(column) + (row + 1);
        }
    }

    /** Where each slot's row is, per category and date, so a booking can find its cell. */
    private record TableLayout(int firstColumn, Map<String, Integer> rowsBySlot) {}

    private record State(EquipmentSnapshot snapshot, String sheetTitle, Map<String, Map<String, TableLayout>> layout) {

        Optional<Cell> locate(String categoryId, String date, String slotId, int unitIndex) {
            var table = layout.getOrDefault(categoryId, Map.of()).get(date);
            if (table == null || unitIndex < 0 || !table.rowsBySlot().containsKey(slotId)) return Optional.empty();
            return Optional.of(new Cell(table.rowsBySlot().get(slotId), table.firstColumn() + 1 + unitIndex));
        }
    }

    private Meta meta() {
        Spreadsheet sheet = sheets.equipment();
        var cached = meta;
        if (cached != null && cached.sheet() == sheet && clock.instant().isBefore(cached.at().plus(META_CACHE))) return cached;

        var tabs = sheet.tabs();
        boolean byName = wantedTab != null && !wantedTab.isBlank();
        var tab = tabs.stream()
                .filter(t -> byName ? t.title().equals(wantedTab) : !t.tables().isEmpty())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(byName
                        ? "Tab \"" + wantedTab + "\" not found" : "No tab with tables found in the sheet"));
        var fresh = new Meta(sheet, tab.title(), tab.tables(), clock.instant());
        meta = fresh;
        return fresh;
    }

    private State read() {
        Meta meta = meta();
        List<List<String>> grid = sheets.equipment().values(Spreadsheet.quote(meta.sheetTitle()));

        Map<String, CategoryState> categories = new LinkedHashMap<>();
        List<Booking> bookings = new ArrayList<>();
        Map<String, Map<String, TableLayout>> layout = new HashMap<>();

        for (var category : config.categories()) {
            Integer quantity = findConfig(grid, category.quantityLabel());
            Integer maxSessions = findConfig(grid, category.maxSessionsLabel());
            if (quantity == null) throw new IllegalStateException("Couldn't find \"" + category.quantityLabel() + "\" in the sheet");
            if (maxSessions == null) throw new IllegalStateException("Couldn't find \"" + category.maxSessionsLabel() + "\" in the sheet");

            record Found(String date, int firstColumn, int slotColumns, List<Slots.Slot> slots, List<Integer> rows) {}
            List<Found> found = new ArrayList<>();
            for (var table : meta.tables()) {
                var m = TABLE_NAME.matcher(table.name());
                if (!m.matches() || !m.group(1).strip().equalsIgnoreCase(category.tablePrefix())) continue;
                int dayNumber = Integer.parseInt(m.group(2));
                if (dayNumber < 1 || dayNumber > config.days().size()) continue;
                String date = config.days().get(dayNumber - 1).date().toString();

                int header = -1;
                for (int r = table.startRow(); r < table.endRow() && header < 0; r++) {
                    if (cell(grid, r, table.startColumn()).equalsIgnoreCase("timeslot")) header = r;
                }
                if (header < 0) continue;

                List<Slots.Slot> slots = new ArrayList<>();
                List<Integer> rows = new ArrayList<>();
                for (int r = header + 1; r < table.endRow(); r++) {
                    String start = Slots.parse(cell(grid, r, table.startColumn()));
                    if (start == null) continue;
                    slots.add(new Slots.Slot(start, start, Slots.addMinutes(start, config.slotMinutes())));
                    rows.add(r);
                }
                found.add(new Found(date, table.startColumn(), table.endColumn() - table.startColumn() - 1, slots, rows));
            }

            // Never offer more units than every day's table has columns for.
            int unitCount = found.isEmpty() ? 0
                    : Math.max(0, Math.min(quantity, found.stream().mapToInt(Found::slotColumns).min().orElse(0)));
            List<String> units = new ArrayList<>();
            for (int i = 1; i <= unitCount; i++) units.add(category.unitName() + " " + i);

            Map<String, List<Slots.Slot>> days = new LinkedHashMap<>();
            Map<String, TableLayout> tables = new HashMap<>();
            for (var f : found) {
                days.put(f.date(), f.slots());
                Map<String, Integer> rowsBySlot = new HashMap<>();
                for (int i = 0; i < f.rows().size(); i++) {
                    int row = f.rows().get(i);
                    String slotId = f.slots().get(i).id();
                    rowsBySlot.put(slotId, row);
                    for (int u = 0; u < unitCount; u++) {
                        String team = cell(grid, row, f.firstColumn() + 1 + u);
                        if (!team.isEmpty()) bookings.add(new Booking(team, category.id(), units.get(u), f.date(), slotId));
                    }
                }
                tables.put(f.date(), new TableLayout(f.firstColumn(), rowsBySlot));
            }
            layout.put(category.id(), tables);
            categories.put(category.id(), new CategoryState(List.copyOf(units), maxSessions, days));
        }
        return new State(new EquipmentSnapshot(categories, List.copyOf(bookings)), meta.sheetTitle(), layout);
    }

    /** The number to the right of a label cell anywhere on the tab (e.g. "Gimbal Quantity" | 3). */
    private static Integer findConfig(List<List<String>> grid, String label) {
        for (int r = 0; r < grid.size(); r++) {
            for (int c = 0; c < grid.get(r).size(); c++) {
                if (!cell(grid, r, c).toLowerCase(Locale.ROOT).equals(label.toLowerCase(Locale.ROOT))) continue;
                String raw = cell(grid, r, c + 1);
                try {
                    double n = Double.parseDouble(raw);
                    return Double.isFinite(n) ? (int) n : null;
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        }
        return null;
    }

    private static String cell(List<List<String>> grid, int row, int column) {
        if (row < 0 || row >= grid.size()) return "";
        var cells = grid.get(row);
        return column < 0 || column >= cells.size() || cells.get(column) == null ? "" : cells.get(column).strip();
    }
}
