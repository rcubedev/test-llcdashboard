package org.limelightcreatives.dashboard.schedule;

import jakarta.servlet.http.HttpServletResponse;
import org.limelightcreatives.dashboard.schedule.RunOfShow.Item;
import org.limelightcreatives.dashboard.schedule.RunOfShow.Kind;
import org.limelightcreatives.dashboard.schedule.RunOfShow.Now;
import org.limelightcreatives.dashboard.schedule.RunOfShow.TimeState;
import org.limelightcreatives.dashboard.schedule.Schedule.Day;
import org.limelightcreatives.dashboard.schedule.Schedule.Staff;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The run of show (content/run-of-show.json): participants see what's on when and where; staff see their own
 * day ("My day", for the person they pick, remembered in a cookie) or everyone at once ("Full view").
 *
 * <p>Query parameters: {@code ?day=sat}, {@code ?person=dev}, {@code ?view=full}, and {@code ?now=2026-10-24T10:30}
 * to see the page as it will look at that moment.
 */
@Controller
public class ScheduleController {

    static final String PERSON_COOKIE = "limelight-ros-person";

    private final RunOfShow ros;
    private final Clock clock;

    public ScheduleController(JsonMapper json, Clock clock) throws IOException {
        try (InputStream in = new ClassPathResource("content/run-of-show.json").getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> raw = json.readValue(in, Map.class);
            this.ros = new RunOfShow(Schedule.fromJson(raw));
        }
        this.clock = clock;
    }

    // ---------- View models ----------

    public record DayTab(String id, String label, boolean active, String href) {}

    /** A line of the participant schedule: a marker (a heading) or a block. */
    public record Entry(boolean marker, String time, String text, String where, String state, String timeRange) {}

    public record NowNext(Entry current, Entry next, String nextTime) {}

    /** A card on a staff member's (or participants') timeline. */
    public record Card(String kind, String time, String state, String title, String location, List<RunOfShow.Role> roles,
                       String note, String subtitle, List<String> participants, String marker) {

        public boolean past() {
            return "past".equals(state);
        }
    }

    public record GridCell(boolean off, List<RunOfShow.Role> roles, String location, String note, String partial, boolean mine) {}

    public record GridRow(boolean marker, String time, String place, String participants, String staffOnly, String state,
                          List<GridCell> cells) {}

    // ---------- Participants ----------

    @GetMapping("/participant/schedule")
    public String participants(@RequestParam(required = false) String day, @RequestParam(required = false) String now,
                               Model model) {
        Now at = ros.now(ZonedDateTime.now(clock), now);
        Day selected = pickDay(day, at);
        model.addAttribute("schedule", ros.data());
        model.addAttribute("days", dayTabs(selected, "/participant/schedule", params("now", now)));
        model.addAttribute("day", selected);

        List<Entry> entries = new ArrayList<>();
        if (selected != null) {
            for (var b : selected.blocks()) {
                int s = RunOfShow.toMinutes(b.start());
                if (b.isMarker()) {
                    entries.add(new Entry(true, RunOfShow.fmt(s), b.participants() != null ? b.participants() : b.title(), null, null, null));
                } else if (b.participants() != null) {
                    int e = RunOfShow.toMinutes(b.end());
                    entries.add(new Entry(false, RunOfShow.fmt(s), b.participants(), ros.place(b.location()),
                            state(RunOfShow.timeState(selected, at, s, e)), RunOfShow.fmtRange(s, e < 24 * 60 ? e : null)));
                }
            }
        }
        boolean live = selected != null && selected.date() != null && at.date().equals(selected.date());
        if (live) {
            var blocks = entries.stream().filter(e -> !e.marker()).toList();
            Entry current = blocks.stream().filter(e -> "now".equals(e.state())).findFirst().orElse(null);
            Entry next = blocks.stream().filter(e -> "future".equals(e.state())).findFirst().orElse(null);
            if (current != null || next != null) model.addAttribute("nowNext", new NowNext(current, next, next == null ? null : next.time()));
        }
        model.addAttribute("entries", entries);
        return "schedule/participant";
    }

    // ---------- Staff ----------

    @GetMapping("/staff/schedule")
    public String staff(@RequestParam(required = false) String person, @RequestParam(required = false) String day,
                        @RequestParam(required = false) String view, @RequestParam(required = false) String now,
                        @RequestParam(required = false) String switchPerson,
                        @CookieValue(name = PERSON_COOKIE, required = false) String remembered,
                        Model model, HttpServletResponse response) {
        Now at = ros.now(ZonedDateTime.now(clock), now);
        String wanted = person == null ? "" : person.toLowerCase(Locale.ROOT);
        String chosen;
        if (switchPerson != null) {
            chosen = null;
            remember(response, null);
        } else if (valid(wanted)) {
            chosen = wanted;
            if (!wanted.equals(remembered)) remember(response, wanted);
        } else {
            chosen = wanted.isEmpty() && valid(remembered) ? remembered : null;
        }
        boolean full = "full".equals(view);
        Day selected = pickDay(day, at);
        boolean hasBlocks = selected != null && !selected.blocks().isEmpty();
        boolean picking = !full && chosen == null;
        boolean live = selected != null && selected.date() != null && at.date().equals(selected.date());
        Staff me = chosen == null ? null : ros.findStaff(chosen).orElse(null);

        Map<String, String> keep = params("view", full ? "full" : null, "now", now);
        model.addAttribute("schedule", ros.data());
        model.addAttribute("days", dayTabs(selected, "/staff/schedule", keep));
        model.addAttribute("day", selected);
        model.addAttribute("full", full);
        model.addAttribute("picking", picking);
        model.addAttribute("hasBlocks", hasBlocks);
        model.addAttribute("who", chosen); // "person" is who's signed in (PageModel)
        model.addAttribute("me", me);
        model.addAttribute("isParticipant", RunOfShow.PARTICIPANT.equals(chosen));
        model.addAttribute("mineHref", link("/staff/schedule", params("day", selected == null ? "" : selected.id(), "now", now)));
        model.addAttribute("fullHref", link("/staff/schedule", params("day", selected == null ? "" : selected.id(), "view", "full", "now", now)));
        model.addAttribute("switchHref", link("/staff/schedule", params("switchPerson", "1", "now", now)));

        if (live && !full && chosen != null && (me == null || me.placeholder() == null) && selected != null) {
            ros.nowNext(selected, chosen, at.min()).ifPresent(b -> model.addAttribute("banner", b));
        }
        if (picking) {
            model.addAttribute("picker", picker(selected, now));
            model.addAttribute("participantHref", link("/staff/schedule",
                    params("person", RunOfShow.PARTICIPANT, "day", selected == null ? "" : selected.id(), "now", now)));
        }
        if (selected != null && hasBlocks && full) {
            model.addAttribute("grid", grid(selected, chosen, at));
            model.addAttribute("staffColumns", ros.scheduled());
        }
        if (selected != null && hasBlocks && !full && !picking && (me == null || me.placeholder() == null)) {
            model.addAttribute("absent", !RunOfShow.PARTICIPANT.equals(chosen) && ros.isAbsent(selected, chosen));
            model.addAttribute("cards", timeline(selected, chosen, me, at));
        }
        return "schedule/staff";
    }

    private boolean valid(String id) {
        return id != null && !id.isEmpty() && (RunOfShow.PARTICIPANT.equals(id) || ros.findStaff(id).isPresent());
    }

    private void remember(HttpServletResponse response, String person) {
        var cookie = ResponseCookie.from(PERSON_COOKIE, person == null ? "" : person)
                .path("/").httpOnly(true).sameSite("Lax")
                .maxAge(person == null ? Duration.ZERO : Duration.ofDays(365));
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.build().toString());
    }

    public record PickerButton(String name, String href) {}

    /** "Who are you?": the lead first, then everyone else by name, mentors, and placeholders last. */
    private List<PickerButton> picker(Day day, String now) {
        String lead = ros.data().event().leadId();
        var scheduled = ros.scheduled();
        List<Staff> ordered = new ArrayList<>();
        scheduled.stream().filter(s -> s.id().equals(lead)).forEach(ordered::add);
        scheduled.stream().filter(s -> !s.id().equals(lead) && !s.id().equals("mentors"))
                .sorted(Comparator.comparing(Staff::name, String.CASE_INSENSITIVE_ORDER)).forEach(ordered::add);
        scheduled.stream().filter(s -> s.id().equals("mentors")).forEach(ordered::add);
        ros.staffList().stream().filter(s -> s.placeholder() != null).forEach(ordered::add);
        String dayId = day == null ? "" : day.id();
        return ordered.stream().map(s -> new PickerButton(s.name(), link("/staff/schedule", params("person", s.id(), "day", dayId, "now", now)))).toList();
    }


    private List<Card> timeline(Day day, String id, Staff person, Now now) {
        var plan = ros.planFor(day, id);
        boolean finished = plan.stream().anyMatch(i -> i.kind == Kind.DONE && RunOfShow.timeState(day, now, i.s, i.e()) == TimeState.NOW);
        List<Card> cards = new ArrayList<>();
        String lastLoc = "";
        for (Item it : plan) {
            if (it.kind == Kind.MARKER) {
                cards.add(new Card("marker", RunOfShow.fmt(it.s), null, null, null, List.of(), null, null, List.of(), it.text()));
                continue;
            }
            TimeState st = RunOfShow.timeState(day, now, it.s, it.e());
            if (finished && it.kind != Kind.DONE) continue;
            String loc = ros.where(it);
            boolean showLoc = !loc.isEmpty() && (!loc.equals(lastLoc) || st != TimeState.PAST);
            if (!loc.isEmpty()) lastLoc = loc;

            String time = RunOfShow.fmtRange(it.s, it.e() < 24 * 60 ? it.e() : null);
            String title, subtitle = null, note = null;
            List<RunOfShow.Role> roles = List.of();
            List<String> participants = List.of();
            switch (it.kind) {
                case ARRIVE, RETURN -> {
                    String where = it.first().a().location();
                    title = (it.kind == Kind.RETURN ? "Back" : "Arrive") + (where != null && !where.isEmpty() ? " at " + where : "")
                            + " by " + RunOfShow.fmt(it.at());
                    subtitle = (it.kind == Kind.RETURN ? "Next" : "First") + " job " + RunOfShow.fmt(it.first().s) + " · "
                            + it.first().a().roleNames();
                }
                case DONE -> title = "You’re done at " + RunOfShow.fmt(it.at()) + ". Thanks, " + (person == null ? "" : person.name()) + ".";
                case PBLOCK -> {
                    title = it.text();
                    note = it.note();
                }
                default -> { // BLOCK
                    title = it.a().roles().isEmpty() ? "Not assigned yet" : null;
                    roles = it.a().roles();
                    note = it.a().note();
                    var ctx = it.ctx();
                    List<String> lines = new ArrayList<>();
                    for (int j = 0; j < ctx.size(); j++) {
                        if (j > 0 && ctx.get(j).text().equals(ctx.get(j - 1).text())) continue;
                        lines.add((ctx.size() > 1 ? RunOfShow.fmt(ctx.get(j).s()) + " " : "") + ctx.get(j).text());
                    }
                    participants = lines;
                }
            }
            cards.add(new Card(it.kind.name().toLowerCase(Locale.ROOT), time, state(st), title, showLoc ? loc : null,
                    roles, note, subtitle, participants, null));
        }
        return cards;
    }

    private List<GridRow> grid(Day day, String me, Now now) {
        var scheduled = ros.scheduled();
        List<GridRow> rows = new ArrayList<>();
        for (var b : day.blocks()) {
            int s = RunOfShow.toMinutes(b.start());
            if (b.isMarker()) {
                rows.add(new GridRow(true, RunOfShow.fmt(s), null, b.participants() != null ? b.participants() : b.title(), null, null, List.of()));
                continue;
            }
            int e = RunOfShow.toMinutes(b.end());
            String blockPlace = ros.place(b.staffLocation() != null ? b.staffLocation() : b.location());
            List<GridCell> cells = new ArrayList<>();
            for (var p : scheduled) {
                var a = ros.assignment(b, p, day);
                boolean mine = p.id().equals(me);
                if (a.off()) {
                    cells.add(new GridCell(true, List.of(), null, null, a.partial(), mine));
                } else {
                    String where = a.location() != null && !a.location().isEmpty() && !a.location().equals(blockPlace) ? a.location() : null;
                    cells.add(new GridCell(false, a.roles(), where, a.note(), a.partial(), mine));
                }
            }
            rows.add(new GridRow(false, RunOfShow.fmtRange(s, e), ros.place(b.location()), b.participants(), b.title(),
                    state(RunOfShow.timeState(day, now, s, e)), cells));
        }
        return rows;
    }

    // ---------- Helpers ----------

    /** ?day= if it's a day, else today if the event is on, else the first day. */
    private Day pickDay(String id, Now now) {
        var days = ros.data().days();
        return days.stream().filter(d -> d.id().equals(id)).findFirst()
                .or(() -> days.stream().filter(d -> now.date().equals(d.date())).findFirst())
                .orElse(days.isEmpty() ? null : days.getFirst());
    }

    private List<DayTab> dayTabs(Day selected, String path, Map<String, String> keep) {
        return ros.data().days().stream().map(d -> {
            Map<String, String> params = new java.util.LinkedHashMap<>(keep);
            params.put("day", d.id());
            return new DayTab(d.id(), d.label() + " · " + RunOfShow.fmtShortDate(d.date()), d == selected, link(path, params));
        }).toList();
    }

    /** Query parameters in order; null values are left out of links. */
    static Map<String, String> params(String... keysAndValues) {
        Map<String, String> params = new java.util.LinkedHashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2) params.put(keysAndValues[i], keysAndValues[i + 1]);
        return params;
    }

    static String link(String path, Map<String, String> params) {
        var url = UriComponentsBuilder.fromPath(path);
        List<String> values = new ArrayList<>();
        params.forEach((k, v) -> {
            if (v != null && !v.isEmpty()) {
                url.queryParam(k, "{" + k + "}");
                values.add(v);
            }
        });
        return url.encode().buildAndExpand(values.toArray()).toUriString();
    }

    private static String state(TimeState st) {
        return st.name().toLowerCase(Locale.ROOT);
    }
}
