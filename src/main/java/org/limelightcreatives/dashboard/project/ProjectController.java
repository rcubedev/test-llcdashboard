package org.limelightcreatives.dashboard.project;

import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * An event dashboard's Project tab: the team's project, or the form for it. The form shows when there's no
 * project yet, after Edit ({@code ?edit=true}), and again with what they typed when saving didn't work.
 */
@Controller
@RequestMapping("/participant/{eventId}/project")
class ProjectController {

    /** Where teams upload their film and files before linking it here. */
    static final String DRIVE_UPLOAD_URL = "https://drive.google.com/drive/folders/1AQ2uSyC7Hx1pd7snZvLhLEv4CrRXbLPm";

    private final Teams teams;
    private final Projects projects;

    ProjectController(Teams teams, Projects projects) {
        this.teams = teams;
        this.projects = projects;
    }

    @GetMapping
    String project(EventConfig event, Person person, @RequestParam(required = false) String edit, Model model) {
        var team = this.teams.of(person.id());
        if (team.isEmpty()) return "event/project";

        Project saved = this.projects.of(team.get().id()).orElse(null);
        model.addAttribute("hasTeam", true);
        model.addAttribute("project", saved);
        model.addAttribute("closed", this.projects.closed());
        model.addAttribute("driveUrl", DRIVE_UPLOAD_URL);
        // a failed save leaves "form" (what they typed) in the model already
        boolean editing = saved == null || edit != null || model.containsAttribute("form");
        model.addAttribute("editing", editing && !this.projects.closed());
        if (!model.containsAttribute("form")) model.addAttribute("form", saved == null ? Project.EMPTY : saved);
        return "event/project";
    }

    @PostMapping
    String save(EventConfig event, Person person, @RequestParam(defaultValue = "") String title,
                @RequestParam(defaultValue = "") String logline, @RequestParam(defaultValue = "") String videoUrl,
                RedirectAttributes redirect) {
        try {
            this.projects.save(person.id(), title, logline, videoUrl);
        } catch (ProjectException e) {
            redirect.addFlashAttribute("error", e.getMessage());
            redirect.addFlashAttribute("form", new Project(title, logline, videoUrl));
        }
        return "redirect:/participant/" + event.id() + "/project";
    }
}
