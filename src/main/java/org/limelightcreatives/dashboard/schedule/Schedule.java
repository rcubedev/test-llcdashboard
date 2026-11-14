package org.limelightcreatives.dashboard.schedule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The run of show, from content/run-of-show.json: who does what, when and where, for staff and participants.
 * The file's format is the old app's, unchanged; see that file for an example of every field.
 */
public record Schedule(Event event, Map<String, String> locations, List<Travel> travel, List<Staff> staff,
                       Map<String, RoleDef> roles, List<Day> days) {

    /**
     * @param timezone     the event's time zone (decides what "now" is)
     * @param leadId       the staff member in charge: listed first, and doesn't get the redirect note
     * @param redirectNote shown on everyone else's timeline, e.g. "Ontik may redirect you on the day."
     */
    public record Event(String name, String subtitle, String timezone, String leadId, String redirectNote) {}

    /** Walking time between two venues, in minutes. */
    public record Travel(String a, String b, int minutes) {}

    /**
     * Someone on the run of show.
     *
     * @param placeholder  shown instead of a timeline until assignments are confirmed
     * @param availability by day id: when they're on site, as a list of [start, end] spans (empty = all day),
     *                     or null for not on site that day. Days not listed: all day.
     */
    public record Staff(String id, String name, String placeholder, String note, Map<String, List<Span>> availability) {}

    /** "07:30" to "19:00", as minutes past midnight. */
    public record Span(int start, int end) {}

    /**
     * What a role involves.
     *
     * @param location where it happens, as a location code; overrides the block's location
     * @param flexible marked "Flex" on the page: they may be moved where needed
     */
    public record RoleDef(List<String> steps, String location, boolean flexible) {}

    public record Day(String id, String label, String date, List<Block> blocks) {}

    /**
     * One row of the day. A block without an end is a marker (a heading such as "Lunch").
     *
     * @param location            location code(s), e.g. "LT" or "LT → GA"; expanded with {@code locations}
     * @param participants        what participants are doing; blocks without it are staff-only
     * @param staff               what each staff member does, by staff id
     * @param allStaff            what everyone not listed in {@code staff} does
     */
    public record Block(String start, String end, String location, String participantLocation, String staffLocation,
                        String participants, String participantNote, String title, Map<String, Given> staff,
                        Given allStaff) {

        public boolean isMarker() {
            return end == null;
        }
    }

    /** A staff member's job in a block: one or more roles, maybe somewhere else, maybe with a note. */
    public record Given(List<String> roles, String location, String note) {}

    // ---------- Reading the JSON (parsed as plain maps and lists) ----------

    static Schedule fromJson(Map<String, Object> json) {
        Map<String, Object> event = map(json.get("event"));
        Map<String, String> locations = new LinkedHashMap<>();
        map(json.get("locations")).forEach((k, v) -> locations.put(k, String.valueOf(v)));

        List<Travel> travel = new ArrayList<>();
        for (Object t : list(json.get("travel"))) {
            var between = list(map(t).get("between"));
            if (between.size() == 2) travel.add(new Travel(str(between.get(0)), str(between.get(1)), num(map(t).get("minutes"))));
        }

        List<Staff> staff = new ArrayList<>();
        for (Object s : list(json.get("staff"))) {
            var m = map(s);
            Map<String, List<Span>> availability = new LinkedHashMap<>();
            map(m.get("availability")).forEach((day, raw) -> availability.put(day, spans(raw)));
            staff.add(new Staff(str(m.get("id")), str(m.get("name")), str(m.get("placeholder")), str(m.get("note")), availability));
        }

        Map<String, RoleDef> roles = new LinkedHashMap<>();
        map(json.get("roles")).forEach((name, r) -> {
            var m = map(r);
            roles.put(name, new RoleDef(list(m.get("steps")).stream().map(String::valueOf).toList(), str(m.get("location")),
                    Boolean.TRUE.equals(m.get("flexible"))));
        });

        List<Day> days = new ArrayList<>();
        for (Object d : list(json.get("days"))) {
            var m = map(d);
            List<Block> blocks = new ArrayList<>();
            for (Object b : list(m.get("blocks"))) {
                var bm = map(b);
                Map<String, Given> given = new LinkedHashMap<>();
                map(bm.get("staff")).forEach((id, raw) -> given.put(id, given(raw)));
                blocks.add(new Block(str(bm.get("start")), str(bm.get("end")), str(bm.get("location")),
                        str(bm.get("participantLocation")), str(bm.get("staffLocation")), str(bm.get("participants")),
                        str(bm.get("participantNote")), str(bm.get("title")), given, given(bm.get("allStaff"))));
            }
            days.add(new Day(str(m.get("id")), str(m.get("label")), str(m.get("date")), blocks));
        }

        return new Schedule(new Event(str(event.get("name")), str(event.get("subtitle")),
                str(event.get("timezone")) == null ? "Australia/Sydney" : str(event.get("timezone")),
                str(event.get("leadId")), str(event.get("redirectNote"))),
                locations, travel, staff, roles, days);
    }

    /** {@code false} → null (not on site); {@code ["07:30","19:00"]} or a list of those → spans; missing or empty → all day. */
    private static List<Span> spans(Object raw) {
        if (Boolean.FALSE.equals(raw)) return null;
        var list = list(raw);
        if (list.isEmpty()) return List.of();
        List<List<?>> pairs = list.get(0) instanceof String ? List.of(list) : list.stream().map(Schedule::list).<List<?>>map(l -> l).toList();
        return pairs.stream()
                .map(p -> new Span(RunOfShow.toMinutes(str(p.get(0))), RunOfShow.toMinutes(str(p.get(1)))))
                .sorted((x, y) -> Integer.compare(x.start(), y.start()))
                .toList();
    }

    /** A role name, a list of them, or {@code {role|roles, location, note}}. */
    private static Given given(Object raw) {
        if (raw == null) return null;
        if (raw instanceof String || raw instanceof List<?>) return new Given(names(raw), null, null);
        var m = map(raw);
        return new Given(names(m.get("role") != null ? m.get("role") : m.get("roles")), str(m.get("location")), str(m.get("note")));
    }

    private static List<String> names(Object raw) {
        if (raw == null || "".equals(raw)) return List.of();
        if (raw instanceof List<?> list) return list.stream().map(String::valueOf).toList();
        return List.of(String.valueOf(raw));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> map(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();
    }

    private static List<?> list(Object o) {
        return o instanceof List<?> l ? l : List.of();
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static int num(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
