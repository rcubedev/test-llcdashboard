package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.event.Events;
import org.limelightcreatives.dashboard.event.Milestone;
import org.limelightcreatives.dashboard.milestone.MilestoneSubmissions.Decision;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /staff/milestones}: reviewing milestone submissions (approving one unlocks the team's next milestone),
 * and every team's progress at a glance.
 *
 * <p>The view, filter and search are in the address ({@code ?view=progress}, {@code ?filter=all&q=gaffers}), so
 * they survive a refresh and each decision comes back to the same list.
 */
@Controller
@RequestMapping("/staff/milestones")
public class StaffMilestonesController {

    private final MilestoneSubmissions submissions;
    private final Events events;
    private final DashboardProperties properties;

    StaffMilestonesController(MilestoneSubmissions submissions, Events events, DashboardProperties properties) {
        this.submissions = submissions;
        this.events = events;
        this.properties = properties;
    }

    /** A team's row in the progress table. */
    public record TeamProgress(String id, String name, Map<String, MilestoneStatus> statuses, int approved) {}

    /** A column of the progress table: a milestone someone has submitted. */
    public record Column(String id, String label) {}

    @GetMapping
    String milestones(@RequestParam(defaultValue = "review") String view,
                      @RequestParam(defaultValue = "pending") String filter,
                      @RequestParam(defaultValue = "") String q, Model model) {
        List<Submission> rows = this.submissions.all();
        long pending = rows.stream().filter(r -> !r.approved()).count();

        model.addAttribute("view", view.equals("progress") ? "progress" : "review");
        model.addAttribute("filter", List.of("pending", "approved", "all").contains(filter) ? filter : "pending");
        model.addAttribute("q", q);
        model.addAttribute("total", rows.size());
        model.addAttribute("pendingCount", pending);
        model.addAttribute("approvedCount", rows.size() - pending);
        model.addAttribute("visible", visible(rows, (String) model.getAttribute("filter"), q));
        model.addAttribute("prizes", this.events.prizes());
        model.addAttribute("resultsUrl", this.properties.fillout().resultsUrl());

        var columns = columns(rows);
        model.addAttribute("columns", columns);
        model.addAttribute("teams", teams(rows));
        return "staff/milestones";
    }

    @PostMapping("/review")
    String review(Person person, @RequestParam String teamId, @RequestParam String milestoneId,
                  @RequestParam Decision decision, @RequestParam(defaultValue = "pending") String filter,
                  @RequestParam(defaultValue = "") String q, RedirectAttributes redirect) {
        String reviewer = person.email() == null || person.email().isBlank() ? person.id() : person.email();
        boolean done;
        try {
            done = this.submissions.review(teamId, milestoneId, decision, reviewer);
        } catch (RuntimeException e) {
            done = false;
        }
        if (!done) redirect.addFlashAttribute("error", "That submission changed in the meantime. Refreshed the list.");
        return "redirect:" + UriComponentsBuilder.fromPath("/staff/milestones")
                .queryParam("filter", "{filter}").queryParam("q", "{q}")
                .encode().buildAndExpand(filter, q).toUriString();
    }

    /** The review list: the filter, then the search (team or milestone). Pending ones oldest first, which is fairer. */
    static List<Submission> visible(List<Submission> rows, String filter, String q) {
        String query = q.strip().toLowerCase(Locale.ROOT);
        var list = new ArrayList<>(rows.stream()
                .filter(r -> switch (filter) {
                    case "pending" -> !r.approved();
                    case "approved" -> r.approved();
                    default -> true;
                })
                .filter(r -> query.isEmpty() || r.teamName().toLowerCase(Locale.ROOT).contains(query)
                        || r.milestoneLabel().toLowerCase(Locale.ROOT).contains(query))
                .toList());
        if (filter.equals("pending")) java.util.Collections.reverse(list);
        return list;
    }

    /** Every team that has submitted something, the most approved first. */
    static List<TeamProgress> teams(List<Submission> rows) {
        Map<String, TeamProgress> byId = new LinkedHashMap<>();
        for (Submission r : rows) {
            var team = byId.computeIfAbsent(r.teamId(), id -> new TeamProgress(id, r.teamName(), new HashMap<>(), 0));
            team.statuses().put(r.milestoneId(), r.status());
        }
        return byId.values().stream()
                .map(t -> new TeamProgress(t.id(), t.name(), t.statuses(),
                        (int) t.statuses().values().stream().filter(s -> s == MilestoneStatus.APPROVED).count()))
                .sorted(Comparator.comparingInt(TeamProgress::approved).reversed()
                        .thenComparing(TeamProgress::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Every milestone someone has submitted, in its event's order (the old page sorted them by id, which put
     * "final-cut" first), then any the events no longer list.
     */
    List<Column> columns(List<Submission> rows) {
        Map<String, Integer> order = new HashMap<>();
        for (EventConfig event : this.events.list()) {
            for (Milestone m : event.ordered()) order.putIfAbsent(m.id(), order.size());
        }
        Map<String, String> labels = new LinkedHashMap<>();
        for (Submission r : rows) labels.putIfAbsent(r.milestoneId(), r.milestoneLabel());
        return labels.entrySet().stream()
                .map(e -> new Column(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingInt((Column c) -> order.getOrDefault(c.id(), Integer.MAX_VALUE))
                        .thenComparing(Column::id))
                .toList();
    }
}
