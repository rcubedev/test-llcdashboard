package org.limelightcreatives.dashboard.web;

import org.limelightcreatives.dashboard.account.Person;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** The launchpads, and the apps that aren't ready yet. */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Person person) {
        return person.isStaff() ? "redirect:/staff" : "redirect:/participant";
    }

    @GetMapping("/participant")
    public String participant(Model model) {
        model.addAttribute("apps", App.PARTICIPANT);
        return "launchpad";
    }

    @GetMapping("/staff")
    public String staff(Model model) {
        model.addAttribute("apps", App.STAFF);
        return "launchpad";
    }

    /** Linked from nowhere yet, as in the old app. */
    @GetMapping({"/staff/theatre", "/staff/presenter"})
    public String notYet() {
        return "not-yet";
    }
}
