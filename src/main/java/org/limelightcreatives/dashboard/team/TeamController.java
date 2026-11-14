package org.limelightcreatives.dashboard.team;

import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * An event dashboard's Team tab: your team and its join code, or forms to join or create one. Every form posts
 * and comes back here, with what went wrong if anything did.
 */
@Controller
@RequestMapping("/participant/{eventId}/team")
class TeamController {

    private final Teams teams;

    TeamController(Teams teams) {
        this.teams = teams;
    }

    @GetMapping
    String team(EventConfig event, Person person, Model model) {
        model.addAttribute("team", this.teams.of(person.id()).orElse(null));
        model.addAttribute("locked", this.teams.locked());
        return "event/team";
    }

    @PostMapping("/create")
    String create(EventConfig event, Person person, @RequestParam(defaultValue = "") String name,
                  RedirectAttributes redirect) {
        return attempt(event, redirect, () -> this.teams.create(person.id(), name), "teamName", name);
    }

    @PostMapping("/join")
    String join(EventConfig event, Person person, @RequestParam(defaultValue = "") String code,
                RedirectAttributes redirect) {
        return attempt(event, redirect, () -> this.teams.join(person.id(), code), "joinCode", code);
    }

    @PostMapping("/leave")
    String leave(EventConfig event, Person person, RedirectAttributes redirect) {
        return attempt(event, redirect, () -> this.teams.leave(person.id()), null, null);
    }

    /** Does it, or keeps what they typed and why it didn't work for the page. */
    private String attempt(EventConfig event, RedirectAttributes redirect, Runnable change, String field, String typed) {
        try {
            change.run();
        } catch (TeamException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            if (field != null) redirect.addFlashAttribute(field, typed);
        }
        return "redirect:/participant/" + event.id() + "/team";
    }
}
