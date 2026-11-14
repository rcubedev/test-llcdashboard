package org.limelightcreatives.dashboard.schedule;

import org.limelightcreatives.dashboard.schedule.Schedule.Block;
import org.limelightcreatives.dashboard.schedule.Schedule.Day;
import org.limelightcreatives.dashboard.schedule.Schedule.Given;
import org.limelightcreatives.dashboard.schedule.Schedule.Span;
import org.limelightcreatives.dashboard.schedule.Schedule.Staff;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Works out each person's day from the run of show: which roles they have when, where, merged into
 * cards, with arrive/back/done cards around their time on site, and the "Now / Next / Walk" banner.
 * A port of the old app's lib/run-of-show/schedule.ts; same rules.
 */
public class RunOfShow {

    public static final String PARTICIPANT = "participant";
    private static final int END_OF_DAY = 24 * 60;

    private final Schedule data;
    private final UnaryOperator<String> place;

    public RunOfShow(Schedule data) {
        this.data = data;
        this.place = placeFn(data);
    }

    public Schedule data() {
        return data;
    }

    // ---------- Formatting ----------

    public static int toMinutes(String time) {
        String[] parts = time.split(":");
        return Integer.parseInt(parts[0]) * 60 + (parts.length > 1 && !parts[1].isEmpty() ? Integer.parseInt(parts[1]) : 0);
    }

    /** 810 → "1:30pm" */
    public static String fmt(int min) {
        int h = min / 60, m = min % 60;
        return (h % 12 == 0 ? 12 : h % 12) + ":" + String.format("%02d", m) + (h >= 12 ? "pm" : "am");
    }

    public static String fmtRange(int s, Integer e) {
        return e == null ? fmt(s) : fmt(s) + " – " + fmt(e);
    }

    public static String fmtDuration(int m) {
        return m >= 60 ? (m / 60) + "h" + (m % 60 != 0 ? " " + (m % 60) + "m" : "") : m + "m";
    }

    /** "2026-10-24" → "24 Oct" */
    public static String fmtShortDate(String iso) {
        if (iso == null) return "";
        return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("en-AU")));
    }

    /** "LT / GA" → "La Trobe / Golden Age Cinema" */
    static UnaryOperator<String> placeFn(Schedule data) {
        var map = data.locations();
        if (map.isEmpty()) return t -> t == null ? "" : t;
        String keys = map.keySet().stream()
                .sorted(Comparator.comparingInt(String::length).reversed())
                .map(Pattern::quote)
                .collect(Collectors.joining("|"));
        Pattern re = Pattern.compile("\\b(" + keys + ")\\b");
        return t -> t == null ? "" : re.matcher(t).replaceAll(r -> Matcher.quoteReplacement(map.get(r.group(1))));
    }

    public String place(String text) {
        return place.apply(text);
    }

    // ---------- Time ----------

    /** A date and minutes past midnight in the event's time zone. */
    public record Now(String date, int min) {}

    /** Now in the event's time zone, or the {@code ?now=YYYY-MM-DDTHH:MM} override for trying out a day. */
    public Now now(ZonedDateTime clock, String override) {
        if (override != null && !override.isBlank()) {
            String[] parts = override.split("T", 2);
            try {
                return new Now(parts[0], toMinutes(parts.length > 1 ? parts[1] : "00:00"));
            } catch (NumberFormatException notATime) {
                // a mistyped ?now= shows the real time rather than an error
            }
        }
        var here = clock.withZoneSameInstant(ZoneId.of(data.event().timezone()));
        return new Now(here.toLocalDate().toString(), here.getHour() * 60 + here.getMinute());
    }

    public enum TimeState { PAST, NOW, FUTURE }

    public static TimeState timeState(Day day, Now now, int s, Integer e) {
        if (day.date() == null || !now.date().equals(day.date())) {
            return now.date().compareTo(day.date() == null ? "9" : day.date()) > 0 ? TimeState.PAST : TimeState.FUTURE;
        }
        if (e == null) return now.min() > s ? TimeState.PAST : TimeState.FUTURE;
        return now.min() >= e ? TimeState.PAST : now.min() >= s ? TimeState.NOW : TimeState.FUTURE;
    }

    // ---------- People ----------

    public List<Staff> staffList() {
        return data.staff();
    }

    /** Everyone with a real timeline (not a placeholder). */
    public List<Staff> scheduled() {
        return data.staff().stream().filter(s -> s.placeholder() == null).toList();
    }

    public Optional<Staff> findStaff(String id) {
        return data.staff().stream().filter(s -> s.id().equals(id)).findFirst();
    }

    /** True when the person is not on site at all that day. */
    public boolean isAbsent(Day day, String id) {
        return findStaff(id).map(p -> {
            var w = windowsFor(p, day);
            return w != null && w.isEmpty();
        }).orElse(false);
    }

    /** When someone is on site that day. null = all day, empty = not on site at all. */
    private static List<Span> windowsFor(Staff p, Day day) {
        if (!p.availability().containsKey(day.id())) return null;
        var spans = p.availability().get(day.id());
        if (spans == null) return List.of();
        return spans.isEmpty() ? null : spans;
    }

    // ---------- Assignments ----------

    /** A role, with what it involves. */
    public record Role(String name, List<String> steps, boolean flexible, String location) {}

    /**
     * What one person does in one block.
     *
     * @param off      not on site (or nothing to do while arriving/leaving)
     * @param location where, in words
     * @param code     where, as a location code (to work out walks)
     * @param partial  e.g. "Arrives 9:30am" when they're only there for part of the block
     */
    public record Assignment(boolean off, List<Role> roles, String location, String code, String note, String partial) {

        static Assignment off(String partial) {
            return new Assignment(true, List.of(), null, null, null, partial);
        }

        public String roleNames() {
            return roles.stream().map(Role::name).collect(Collectors.joining(" + "));
        }
    }

    private Role roleInfo(String name) {
        var r = data.roles().get(name);
        return r == null ? new Role(name, List.of(), false, null) : new Role(name, r.steps(), r.flexible(), r.location());
    }

    public Assignment assignment(Block b, Staff p, Day day) {
        int s = toMinutes(b.start()), e = toMinutes(b.end());
        var windows = windowsFor(p, day);
        String partial = null;
        if (windows != null) {
            int i = -1;
            for (int k = 0; k < windows.size() && i < 0; k++) if (s < windows.get(k).end() && e > windows.get(k).start()) i = k;
            if (i < 0) return Assignment.off(null);
            var w = windows.get(i);
            if (s < w.start()) partial = (i > 0 ? "Back " : "Arrives ") + fmt(w.start());
            else if (e > w.end()) partial = "Leaves " + fmt(w.end());
        }
        Given given = b.staff().containsKey(p.id()) ? b.staff().get(p.id()) : b.allStaff();
        if (given == null || given.roles().isEmpty()) {
            return partial != null ? Assignment.off(partial)
                    : new Assignment(false, List.of(), place(first(b.staffLocation(), b.location())), null, null, null);
        }
        var roles = given.roles().stream().map(this::roleInfo).toList();
        String code = first(given.location(), roles.stream().map(Role::location).filter(Objects::nonNull).findFirst().orElse(null),
                b.staffLocation(), b.location());
        return new Assignment(false, roles, place(code), code, given.note(), partial);
    }

    // ---------- Plans (a person's cards for the day) ----------

    public enum Kind { MARKER, BLOCK, PBLOCK, ARRIVE, RETURN, DONE }

    /** One card (or marker line) on a timeline. Mutable while the plan is built, as cards merge. */
    public static final class Item {
        public final Kind kind;
        public final int s;
        Integer e;
        String text;
        String where;
        String code;
        String note;
        String key;
        Assignment a;
        final List<Ctx> ctx = new ArrayList<>();
        Integer at;
        Item first;

        Item(Kind kind, int s, Integer e) {
            this.kind = kind;
            this.s = s;
            this.e = e;
        }

        public Integer e() { return e; }
        public String text() { return text; }
        public String where() { return where; }
        public String code() { return code; }
        public String note() { return note; }
        public Assignment a() { return a; }
        public List<Ctx> ctx() { return ctx; }
        public Integer at() { return at; }
        public Item first() { return first; }
    }

    /** What participants are doing during a staff card, from {@code s}. */
    public record Ctx(int s, String text) {}

    public List<Item> planFor(Day day, String id) {
        if (PARTICIPANT.equals(id)) return participantPlan(day);
        return findStaff(id).map(p -> personalPlan(day, p)).orElse(List.of());
    }

    private List<Item> personalPlan(Day day, Staff p) {
        var blocks = day.blocks().stream().filter(b -> !b.isMarker()).toList();
        if (blocks.isEmpty()) return List.of();
        int dayStart = blocks.stream().mapToInt(b -> toMinutes(b.start())).min().orElse(0);
        int dayEnd = blocks.stream().mapToInt(b -> toMinutes(b.end())).max().orElse(0);
        var found = windowsFor(p, day);
        List<Span> windows = found != null ? found : List.of(new Span(dayStart, dayEnd));

        List<Item> items = new ArrayList<>();
        for (Block b : day.blocks()) {
            int s0 = toMinutes(b.start());
            if (b.isMarker()) {
                if (b.participants() != null && windows.stream().anyMatch(w -> s0 >= w.start() && s0 < w.end())) {
                    var marker = new Item(Kind.MARKER, s0, null);
                    marker.text = b.participants();
                    items.add(marker);
                }
                continue;
            }
            var a = assignment(b, p, day);
            if (a.off()) continue;
            int end = toMinutes(b.end());
            Span w = windows.stream().filter(x -> s0 < x.end() && end > x.start()).findFirst().orElseThrow();
            int s = Math.max(s0, w.start()), e = Math.min(end, w.end());
            List<Ctx> ctx = b.participants() != null ? List.of(new Ctx(s, b.participants())) : List.of();
            String key = a.roleNames() + "\u0000" + a.location() + "\u0000" + a.note();
            Item prev = items.isEmpty() ? null : items.getLast();
            if (prev != null && prev.kind == Kind.BLOCK && prev.key.equals(key) && prev.e == s) {
                prev.e = e;
                prev.ctx.addAll(ctx);
                continue;
            }
            var item = new Item(Kind.BLOCK, s, e);
            item.key = key;
            item.a = a;
            item.ctx.addAll(ctx);
            items.add(item);
        }

        var jobs = items.stream().filter(i -> i.kind == Kind.BLOCK).toList();
        if (jobs.isEmpty()) return items;
        for (int i = 0; i < windows.size(); i++) {
            Span w = windows.get(i);
            Item first = jobs.stream().filter(j -> j.s >= w.start()).findFirst().orElse(null);
            if (first == null || (i == 0 && w.start() <= dayStart)) continue;
            int at = -1;
            for (int k = 0; k < items.size() && at < 0; k++) if (items.get(k).s >= w.start()) at = k;
            var card = new Item(i > 0 ? Kind.RETURN : Kind.ARRIVE, w.start(), Math.max(first.s, w.start()));
            card.at = w.start();
            card.first = first;
            items.add(at < 0 ? items.size() : at, card);
        }
        int finish = jobs.getLast().e;
        var done = new Item(Kind.DONE, finish, END_OF_DAY);
        done.at = finish;
        items.add(done);
        return items;
    }

    private List<Item> participantPlan(Day day) {
        List<Item> items = new ArrayList<>();
        for (Block b : day.blocks()) {
            if (b.participants() == null) continue;
            int s = toMinutes(b.start());
            String where = place(first(b.participantLocation(), b.location()));
            if (b.isMarker()) {
                var marker = new Item(Kind.MARKER, s, null);
                marker.text = b.participants();
                items.add(marker);
                continue;
            }
            int e = toMinutes(b.end());
            Item prev = items.isEmpty() ? null : items.getLast();
            if (prev != null && prev.kind == Kind.PBLOCK && prev.e == s && prev.text.equals(b.participants())
                    && prev.where.equals(where) && Objects.equals(prev.note, b.participantNote())) {
                prev.e = e;
                continue;
            }
            var item = new Item(Kind.PBLOCK, s, e);
            item.text = b.participants();
            item.where = where;
            item.code = first(b.participantLocation(), b.location());
            item.note = b.participantNote();
            items.add(item);
        }
        return items;
    }

    // ---------- Describing cards ----------

    public String title(Item it) {
        return switch (it.kind) {
            case BLOCK -> it.a.roleNames().isEmpty() ? "Not assigned" : it.a.roleNames();
            case PBLOCK, MARKER -> it.text;
            case DONE -> "Done";
            case RETURN -> "Back";
            case ARRIVE -> "Arrive";
        };
    }

    public String where(Item it) {
        return switch (it.kind) {
            case BLOCK -> it.a.location() == null ? "" : it.a.location();
            case PBLOCK -> it.where;
            default -> "";
        };
    }

    private String code(Item it) {
        return switch (it.kind) {
            case BLOCK -> it.a.code();
            case PBLOCK -> it.code;
            default -> null;
        };
    }

    /** "LT → GA-front" → "GA" (the venue someone ends up at). */
    private String venueOf(String c) {
        String[] hops = (c == null ? "" : c).split("→");
        String last = hops.length == 0 ? "" : hops[hops.length - 1].strip();
        String base = last.split("-")[0];
        return data.locations().containsKey(base) ? base : last;
    }

    private Integer travel(String a, String b) {
        return data.travel().stream()
                .filter(t -> (t.a().equals(a) && t.b().equals(b)) || (t.a().equals(b) && t.b().equals(a)))
                .map(Schedule.Travel::minutes)
                .findFirst().orElse(null);
    }

    /** The "Now / Next / Walk" banner lines for one person. */
    public record Banner(String now, String nextLine, String walk) {}

    public Optional<Banner> nowNext(Day day, String id, int nowMin) {
        var plan = planFor(day, id);
        var cards = plan.stream().filter(i -> i.kind != Kind.MARKER).toList();
        var jobs = plan.stream().filter(i -> i.kind == Kind.BLOCK || i.kind == Kind.PBLOCK).toList();
        if (jobs.isEmpty()) return Optional.empty();

        Item cur = cards.stream().filter(i -> nowMin >= i.s && nowMin < i.e).findFirst().orElse(null);
        Item upcoming = cards.stream().filter(c -> c.s > nowMin).findFirst().orElse(null);
        String now;
        Item next = null;
        if (cur == null && nowMin < cards.getFirst().s) {
            now = "Your day starts " + fmt(cards.getFirst().s) + " · in " + fmtDuration(cards.getFirst().s - nowMin);
            next = jobs.getFirst();
        } else if (cur == null && upcoming != null && upcoming.kind == Kind.RETURN) {
            String loc = upcoming.first.a.location();
            now = "Back" + (loc != null && !loc.isEmpty() ? " at " + loc : "") + " by " + fmt(upcoming.at)
                    + " · in " + fmtDuration(upcoming.at - nowMin);
            next = upcoming.first;
        } else if (cur != null && cur.kind == Kind.DONE) {
            now = "You’re done for the day. Thanks!";
        } else if (cur != null && (cur.kind == Kind.ARRIVE || cur.kind == Kind.RETURN)) {
            now = "On site · first job " + fmt(cur.first.s);
            next = cur.first;
        } else if (cur != null) {
            String where = where(cur);
            now = title(cur) + (where.isEmpty() ? "" : " · " + where) + " · until " + fmt(cur.e);
            int end = cur.e;
            next = jobs.stream().filter(j -> j.s >= end).findFirst().orElse(null);
        } else {
            next = jobs.stream().filter(j -> j.s > nowMin).findFirst().orElse(null);
            now = next != null ? "Free until " + fmt(next.s) : "Nothing more today";
        }

        String nextLine = "", walk = "";
        if (next != null) {
            int m = next.s - nowMin;
            nextLine = title(next) + " · " + fmt(next.s) + (m > 0 && m <= 90 ? " · in " + m + " min" : "");
            String here = cur != null && cur.kind != Kind.ARRIVE && cur.kind != Kind.RETURN ? venueOf(code(cur)) : "";
            String there = venueOf(code(next));
            if (!here.isEmpty() && !there.isEmpty() && !here.equals(there)) {
                Integer mins = travel(here, there);
                String target = where(next).contains("→") ? place(there) : where(next);
                walk = (mins != null && mins > 0 ? "Walk" : "Head") + " to " + target
                        + (mins != null && mins > 0 ? " (" + mins + " min)" : "");
            }
        } else if (cur != null && cur.kind != Kind.DONE) {
            nextLine = plan.stream().filter(i -> i.kind == Kind.DONE).findFirst().map(d -> "Done at " + fmt(d.at)).orElse("");
        }
        return Optional.of(new Banner(now, nextLine, walk));
    }

    @SafeVarargs
    private static <T> T first(T... values) {
        for (T v : values) if (v != null && !"".equals(v)) return v;
        return null;
    }
}
