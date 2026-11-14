package org.limelightcreatives.dashboard.equipment;

import org.limelightcreatives.dashboard.Times;
import org.limelightcreatives.dashboard.account.Person;
import org.limelightcreatives.dashboard.event.EventConfig;
import org.limelightcreatives.dashboard.team.Teams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * An event dashboard's Equipment tab: book gear in timed slots, per team, from the equipment sheet. The day is
 * in the address ({@code ?day=2026-10-24}); without one it's today when that's an event day, else the first.
 * The team always comes from the database, never the form, so nobody can book as another team.
 */
@Controller
@RequestMapping("/participant/{eventId}/equipment")
class EquipmentController {

    private static final Logger log = LoggerFactory.getLogger(EquipmentController.class);

    private final EquipmentBookings bookings;
    private final EquipmentConfig config;
    private final Teams teams;
    private final Clock clock;

    EquipmentController(EquipmentBookings bookings, EquipmentConfig config, Teams teams, Clock clock) {
        this.bookings = bookings;
        this.config = config;
        this.teams = teams;
        this.clock = clock;
    }

    @GetMapping
    String equipment(EventConfig event, Person person, @RequestParam(required = false) String day, Model model) {
        var team = this.teams.of(person.id());
        if (team.isEmpty()) return "event/equipment";

        EquipmentSnapshot snapshot;
        try {
            snapshot = this.bookings.snapshot();
        } catch (RuntimeException e) {
            log.error("Couldn't read the equipment sheet", e);
            model.addAttribute("loadError", "Couldn't read the equipment sheet");
            return "event/equipment";
        }
        model.addAttribute("view", EquipmentView.of(this.config, snapshot, date(day), team.get().name(), this.clock.instant()));
        model.addAttribute("jitterSeconds", this.config.jitterSeconds());
        return "event/equipment";
    }

    @PostMapping("/reserve")
    String reserve(EventConfig event, Person person, @RequestParam String category, @RequestParam String day,
                   @RequestParam String slot, RedirectAttributes redirect) {
        return change(event, person, day, redirect, (team, date) -> this.bookings.reserve(team, category, date, slot));
    }

    @PostMapping("/cancel")
    String cancel(EventConfig event, Person person, @RequestParam String category, @RequestParam String day,
                  @RequestParam String slot, RedirectAttributes redirect) {
        return change(event, person, day, redirect, (team, date) -> this.bookings.cancel(team, category, date, slot));
    }

    private interface Change {
        void apply(String team, LocalDate date);
    }

    private String change(EventConfig event, Person person, String day, RedirectAttributes redirect, Change change) {
        LocalDate date = date(day);
        try {
            String team = this.teams.of(person.id()).orElseThrow(() -> new BookingException("Join a team before booking")).name();
            change.apply(team, date);
        } catch (BookingException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        } catch (RuntimeException e) {
            log.error("Couldn't update the equipment sheet", e);
            redirect.addFlashAttribute("error", "Couldn't update the sheet. Try again");
        }
        return "redirect:/participant/" + event.id() + "/equipment?day=" + date;
    }

    /** The day asked for if it's one of the event's, else today if that is, else the first. */
    private LocalDate date(String day) {
        var days = this.config.days().stream().map(EquipmentConfig.Day::date).toList();
        if (day != null) {
            try {
                LocalDate asked = LocalDate.parse(day);
                if (days.contains(asked)) return asked;
            } catch (DateTimeParseException ignored) {
                // fall through to the default
            }
        }
        LocalDate today = LocalDate.now(this.clock.withZone(Times.SYDNEY));
        return days.contains(today) || days.isEmpty() ? today : days.getFirst();
    }
}
