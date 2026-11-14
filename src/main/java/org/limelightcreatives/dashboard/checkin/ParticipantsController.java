package org.limelightcreatives.dashboard.checkin;

import org.limelightcreatives.dashboard.checkin.Participants.Input;
import org.limelightcreatives.dashboard.checkin.Participants.Participant;
import org.limelightcreatives.dashboard.checkin.Participants.ParticipantException;
import org.limelightcreatives.dashboard.checkin.Participants.Status;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * {@code /staff/participants}: the participant list in Google Sheets and who's checked in, with adding, editing,
 * deleting and checking people in by hand. Registration IDs can be anything, so they go in the query
 * ({@code /staff/participants/edit?id=LC-001}), not the path.
 */
@Controller
@RequestMapping("/staff/participants")
public class ParticipantsController {

    /** A filter above the list, with how many it shows. */
    public record Filter(String id, String label, long count) {}

    private final Participants participants;
    private final CheckIns checkIns;

    ParticipantsController(Participants participants, CheckIns checkIns) {
        this.participants = participants;
        this.checkIns = checkIns;
    }

    @GetMapping
    String list(@RequestParam(defaultValue = "all") String filter, @RequestParam(defaultValue = "") String q, Model model) {
        List<Participant> rows = rows(model);
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status s : Status.values()) counts.put(s, rows.stream().filter(r -> r.status() == s).count());

        String f = List.of("all", "in", "out", "none").contains(filter) ? filter : "all";
        String query = q.strip().toLowerCase(Locale.ROOT);
        model.addAttribute("filter", f);
        model.addAttribute("q", q);
        model.addAttribute("total", rows.size());
        model.addAttribute("filters", List.of(new Filter("all", "All", rows.size()),
                new Filter("in", "In", counts.get(Status.IN)), new Filter("out", "Out", counts.get(Status.OUT)),
                new Filter("none", "Not arrived", counts.get(Status.NONE))));
        model.addAttribute("visible", rows.stream()
                .filter(r -> f.equals("all") || r.status().name().equalsIgnoreCase(f))
                .filter(r -> query.isEmpty() || List.of(r.name(), r.email(), r.phone(), r.registrationId()).stream()
                        .anyMatch(v -> v.toLowerCase(Locale.ROOT).contains(query)))
                .toList());
        return "staff/participants";
    }

    @GetMapping("/new")
    String create(Model model) {
        if (!model.containsAttribute("draft")) model.addAttribute("draft", Input.EMPTY);
        return "staff/participant-edit";
    }

    @GetMapping("/edit")
    String edit(@RequestParam String id, Model model, RedirectAttributes redirect) {
        if (!model.containsAttribute("draft")) {
            var found = rows(model).stream().filter(p -> p.registrationId().equalsIgnoreCase(id.strip())).findFirst();
            if (found.isEmpty()) {
                redirect.addFlashAttribute("error", model.containsAttribute("error")
                        ? model.getAttribute("error") : "That participant no longer exists. Refresh the list.");
                return "redirect:/staff/participants";
            }
            model.addAttribute("draft", Input.of(found.get()));
        }
        return "staff/participant-edit";
    }

    @PostMapping
    String save(@ModelAttribute Input draft, RedirectAttributes redirect) {
        try {
            this.participants.save(draft);
            return "redirect:/staff/participants";
        } catch (ParticipantException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("draft", draft);
            return draft.isNew() ? "redirect:/staff/participants/new" : "redirect:" + editAddress(draft.originalId());
        }
    }

    @PostMapping("/delete")
    String delete(@RequestParam String id, RedirectAttributes redirect) {
        try {
            this.participants.delete(id);
        } catch (ParticipantException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/staff/participants";
    }

    /** Checks someone in or out by hand: exactly what scanning their code does. */
    @PostMapping("/toggle")
    String toggle(@RequestParam String id, @RequestParam(defaultValue = "all") String filter,
                  @RequestParam(defaultValue = "") String q, RedirectAttributes redirect) {
        var result = this.checkIns.scan(id);
        if (!result.ok()) redirect.addFlashAttribute("error", result.problem());
        return "redirect:" + UriComponentsBuilder.fromPath("/staff/participants")
                .queryParam("filter", "{filter}").queryParam("q", "{q}")
                .encode().buildAndExpand(filter, q).toUriString();
    }

    /** The list, or nobody and why when the sheet didn't answer. */
    private List<Participant> rows(Model model) {
        try {
            return this.participants.list();
        } catch (ParticipantException e) {
            model.addAttribute("error", e.getMessage());
            return List.of();
        }
    }

    static String editAddress(String id) {
        return UriComponentsBuilder.fromPath("/staff/participants/edit").queryParam("id", "{id}")
                .encode().buildAndExpand(id).toUriString();
    }
}
