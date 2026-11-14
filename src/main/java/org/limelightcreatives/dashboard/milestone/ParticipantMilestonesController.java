package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** An event dashboard's Milestones tab. */
@Controller
class ParticipantMilestonesController {

    private final Teams teams;
    private final MilestoneSubmissions submissions;

    ParticipantMilestonesController(Teams teams, MilestoneSubmissions submissions) {
        this.teams = teams;
        this.submissions = submissions;
    }

    @GetMapping("/participant/{eventId}/milestones")
    String milestones(EventConfig event, Person person, Model model) {
        addProgress(event, person, model);
        return "event/milestones";
    }

    /** The team (null when they're not on one) and, if there is one, its progress. */
    void addProgress(EventConfig event, Person person, Model model) {
        var team = this.teams.of(person.id()).orElse(null);
        model.addAttribute("team", team);
        if (team != null) {
            model.addAttribute("progress", new MilestoneProgress(event, team, this.submissions.statusesFor(person.id())));
        }
    }
}
