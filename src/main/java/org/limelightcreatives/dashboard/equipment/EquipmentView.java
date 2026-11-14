package org.limelightcreatives.dashboard.equipment;

import org.limelightcreatives.dashboard.Times;
import org.limelightcreatives.dashboard.equipment.EquipmentSnapshot.Booking;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * One day of the Equipment tab for a team: each category with its daily allowance, and each slot with what the
 * team can do there (the old page's rendering rules, worked out on the server).
 */
public record EquipmentView(LocalDate date, String team, boolean open, String opensAt, Long unlocksAt,
                            String countdown, List<DayTab> days, List<CategoryView> categories) {

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d MMM", Times.AUSTRALIA);

    /** A day to pick, e.g. Saturday 24 Oct. */
    public record DayTab(LocalDate date, String label, String shortDate, boolean active) {}

    /**
     * @param number   01, 02... for a category without a picture
     * @param maxSessions how many slots the team may book in it per day
     */
    public record CategoryView(String id, String label, String description, String image, String number, int units,
                               int maxSessions, int booked, List<SlotView> slots) {

        public boolean limitReached() {
            return booked >= maxSessions;
        }
    }

    /**
     * @param status  Yours, Closed, Full, Limit reached or Available
     * @param myUnit  which unit the team has, e.g. "Gimbal 2"
     * @param units   per unit, the team that has it, or null when it's free
     */
    public record SlotView(String id, String label, String status, String myUnit, List<UnitView> units, int free,
                           boolean canCancel) {

        public boolean mine() {
            return myUnit != null;
        }

        public boolean canReserve() {
            return status.equals("Available");
        }
    }

    public record UnitView(String name, String team) {

        public String title() {
            return team == null ? name + ": free" : name + ": " + team;
        }
    }

    /** {@code team} is the team's name as written in the sheet's cells. */
    static EquipmentView of(EquipmentConfig config, EquipmentSnapshot snapshot, LocalDate date, String team, Instant now) {
        boolean open = config.hasOpened(date, now);
        var opensAt = config.opensAt(date);
        List<DayTab> days = config.days().stream()
                .map(d -> new DayTab(d.date(), d.label(), DAY.format(d.date()), d.date().equals(date)))
                .toList();

        List<CategoryView> categories = new ArrayList<>();
        int index = 0;
        for (var category : config.categories()) {
            index++;
            var state = snapshot.categories().get(category.id());
            List<Slots.Slot> slots = state == null ? List.of() : state.days().getOrDefault(date.toString(), List.of());
            List<String> units = state == null ? List.of() : state.units();
            int max = state == null ? 0 : state.maxSessions();
            List<Booking> today = snapshot.bookings().stream()
                    .filter(b -> b.categoryId().equals(category.id()) && b.date().equals(date.toString()))
                    .toList();
            int booked = (int) today.stream().filter(b -> EquipmentSnapshot.sameTeam(b.team(), team)).count();
            boolean limitReached = booked >= max;

            List<SlotView> slotViews = slots.stream().map(slot -> {
                var here = today.stream().filter(b -> b.slotId().equals(slot.id())).toList();
                var mine = here.stream().filter(b -> EquipmentSnapshot.sameTeam(b.team(), team)).findFirst();
                List<UnitView> unitViews = units.stream()
                        .map(u -> new UnitView(u, here.stream().filter(b -> b.unit().equals(u)).map(Booking::team).findFirst().orElse(null)))
                        .toList();
                int free = (int) unitViews.stream().filter(u -> u.team() == null).count();
                boolean ended = Slots.hasEnded(date, slot, now);
                String status = mine.isPresent() ? "Yours"
                        : ended || !open ? "Closed"
                        : free == 0 ? "Full"
                        : limitReached ? "Limit reached"
                        : "Available";
                return new SlotView(slot.id(), slot.label(), status, mine.map(Booking::unit).orElse(null), unitViews, free,
                        mine.isPresent() && !ended);
            }).toList();

            categories.add(new CategoryView(category.id(), category.label(), category.description(), category.image(),
                    "%02d".formatted(index), units.size(), max, booked, slotViews));
        }

        // the browser enables RESERVE a random moment after opening, to spread the rush (the server checks the real time)
        Long unlocksAt = open || opensAt.isEmpty() ? null : opensAt.get().toEpochMilli();
        String countdown = unlocksAt == null ? "" : countdown(Duration.between(now, opensAt.get()));
        return new EquipmentView(date, team, open, config.formatOpensAt(date), unlocksAt, countdown, days, categories);
    }

    /** How long until booking opens: "2d 3h", "1h 5m" or "4:09" (the same as dashboard.js counts down). */
    static String countdown(Duration left) {
        long s = Math.max(0, (left.toMillis() + 999) / 1000);
        long h = s / 3600, m = (s % 3600) / 60, sec = s % 60;
        if (h >= 24) return (h / 24) + "d " + (h % 24) + "h";
        if (h > 0) return h + "h " + m + "m";
        return m + ":" + "%02d".formatted(sec);
    }

    public String dayLabel() {
        return days.stream().filter(DayTab::active).map(DayTab::label).findFirst().orElse("");
    }
}
