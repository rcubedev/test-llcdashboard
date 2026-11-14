package org.limelightcreatives.dashboard.milestone;

import org.limelightcreatives.dashboard.Times;
import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.Clock;

/** An event dashboard's Home tab: what's up next, the roadmap, and the team's progress. */
@Controller
class EventHomeController {

    private final ParticipantMilestonesController milestones;
    private final Clock clock;

    EventHomeController(ParticipantMilestonesController milestones, Clock clock) {
        this.milestones = milestones;
        this.clock = clock;
    }

    @GetMapping("/participant/{eventId}")
    String home(EventConfig event, Person person, Model model) {
        this.milestones.addProgress(event, person, model);
        model.addAttribute("updatedAt", Times.time(this.clock.instant()));
        return "event/home";
    }
}
