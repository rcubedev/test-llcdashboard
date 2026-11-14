package org.limelightcreatives.dashboard.equipment;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.FakeSpreadsheets;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.team.Teams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Booking gear against the (in-memory) booking grid. The clock is 11:30 am on day 1 (Saturday), so day 1 is
 * open, its 10 AM slot is over, and day 2 opens tomorrow at 9.
 */
@AutoConfigureMockMvc
class EquipmentTest extends DatabaseTest {

    static final LocalDate SAT = LocalDate.parse("2026-10-24");
    static final LocalDate SUN = LocalDate.parse("2026-10-25");
    static final String PAGE = "/participant/film-fest/equipment";

    @Autowired MockMvc mvc;
    @Autowired EquipmentBookings bookings;
    @Autowired Teams teams;

    /** A booking cell in a category's table (see FakeSpreadsheets for the layout). */
    private String cell(int category, int day, int slot, int unit) {
        int units = new int[]{3, 2, 4}[category];
        int top = 8 + category * (FakeSpreadsheets.SLOTS.size() + 3);
        return sheets.equipment().get(FakeSpreadsheets.EQUIPMENT_TAB, top + 1 + slot, day * (units + 2) + 1 + unit);
    }

    @Test
    void readsTheGrid() {
        bookings.forgetCached();
        var snapshot = bookings.snapshot();
        assertThat(snapshot.categories().get("gimbal").units()).containsExactly("Gimbal 1", "Gimbal 2", "Gimbal 3");
        assertThat(snapshot.categories().get("gimbal").maxSessions()).isEqualTo(2);
        assertThat(snapshot.categories().get("mic").units()).containsExactly("Mic 1", "Mic 2");
        assertThat(snapshot.categories().get("tripod").days().get("2026-10-25"))
                .extracting(Slots.Slot::id).containsExactly("10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00");
        assertThat(snapshot.bookings()).isEmpty();
    }

    @Test
    void reservingTakesTheFirstFreeUnit() {
        sheets.equipment().set(FakeSpreadsheets.EQUIPMENT_TAB, 8 + 1 + 1, 1, "Other Team"); // Gimbal 1 at 11:00 on day 1
        bookings.forgetCached();
        var after = bookings.reserve("The Grips", "gimbal", SAT, "11:00");
        assertThat(cell(0, 0, 1, 1)).isEqualTo("The Grips");
        assertThat(after.bookings()).extracting(EquipmentSnapshot.Booking::unit).containsExactlyInAnyOrder("Gimbal 1", "Gimbal 2");
    }

    @Test
    void theRules() {
        bookings.forgetCached();
        bookings.reserve("The Grips", "gimbal", SAT, "11:00");
        assertThatThrownBy(() -> bookings.reserve("the grips ", "gimbal", SAT, "11:00")).hasMessage("Your team already has this slot");
        bookings.reserve("The Grips", "gimbal", SAT, "12:00");
        assertThatThrownBy(() -> bookings.reserve("The Grips", "gimbal", SAT, "13:00"))
                .hasMessage("Your team has reached the limit of 2 for this equipment today");
        assertThatThrownBy(() -> bookings.reserve("The Grips", "gimbal", SAT, "10:00")).hasMessage("That slot has already finished");
        assertThatThrownBy(() -> bookings.reserve("The Grips", "gimbal", SUN, "10:00")).hasMessage("Bookings for this day open 9:00 am");
        assertThatThrownBy(() -> bookings.reserve("The Grips", "drone", SAT, "11:00")).hasMessage("Unknown equipment category");
        assertThatThrownBy(() -> bookings.reserve("The Grips", "mic", SAT, "09:00")).hasMessage("That slot doesn't exist");

        bookings.reserve("A", "mic", SAT, "14:00");
        bookings.reserve("B", "mic", SAT, "14:00");
        assertThatThrownBy(() -> bookings.reserve("C", "mic", SAT, "14:00")).hasMessage("Someone just booked the last one in that slot");

        bookings.cancel("The Grips", "gimbal", SAT, "12:00");
        assertThat(cell(0, 0, 2, 0)).isEmpty();
        assertThatThrownBy(() -> bookings.cancel("The Grips", "gimbal", SAT, "12:00")).hasMessage("Your team doesn't have this slot");
    }

    @Test
    void thePage() throws Exception {
        bookings.forgetCached();
        teams.create("sam", "The Grips");
        var sam = new Visitor(mvc).as(SignedIn.participant("sam"));

        var reserved = sam.post(PAGE + "/reserve", "category", "tripod", "day", "2026-10-24", "slot", "11:00");
        assertThat(reserved.getRedirectedUrl()).isEqualTo(PAGE + "?day=2026-10-24");
        String page = sam.get(PAGE + "?day=2026-10-24").getContentAsString();
        assertThat(page).contains("Booking as <strong>The Grips</strong>", "You have <b>Tripod 1</b>", "Limit reached",
                "Cancel booking", "title=\"Tripod 1: The Grips\"", ">RESERVE</button>")
                .doesNotContain("Join a team first");

        // the tripod limit is 1 a day
        var refused = sam.follow(sam.post(PAGE + "/reserve", "category", "tripod", "day", "2026-10-24", "slot", "12:00"));
        assertThat(refused.getContentAsString()).contains("Your team has reached the limit of 1 for this equipment today");

        sam.post(PAGE + "/cancel", "category", "tripod", "day", "2026-10-24", "slot", "11:00");
        assertThat(sam.get(PAGE + "?day=2026-10-24").getContentAsString()).doesNotContain("You have <b>Tripod 1</b>");

        // tomorrow isn't open yet: the slots show, reserving doesn't
        String sunday = sam.get(PAGE + "?day=2026-10-25").getContentAsString();
        assertThat(sunday).contains("Bookings for", "Sunday", "9:00 am", "data-unlocks-at=\"1792879200000\"", "21h 30m")
                .doesNotContain(">RESERVE</button>");

        // today is the default day
        String today = sam.get(PAGE).getContentAsString();
        assertThat(today).containsPattern("href=\"/participant/film-fest/equipment\\?day=2026-10-24\"[^>]*aria-current=\"page\"");
    }

    @Test
    void noTeamNoBooking() throws Exception {
        bookings.forgetCached();
        var loner = new Visitor(mvc).as(SignedIn.participant("loner"));
        assertThat(loner.get(PAGE).getContentAsString()).contains("Join a team first", "Equipment is booked per team");
        var refused = loner.follow(loner.post(PAGE + "/reserve", "category", "tripod", "day", "2026-10-24", "slot", "11:00"));
        assertThat(refused.getContentAsString()).contains("Join a team first");
        assertThat(bookings.snapshot().bookings()).isEmpty();
    }

    @Test
    void timesAndCountdowns() {
        assertThat(Slots.parse("10:00 AM")).isEqualTo("10:00");
        assertThat(Slots.parse("1:00 pm")).isEqualTo("13:00");
        assertThat(Slots.parse("12:30 a.m.")).isEqualTo("00:30");
        assertThat(Slots.parse("13:00:00")).isEqualTo("13:00");
        assertThat(Slots.parse("9")).isEqualTo("09:00");
        assertThat(Slots.parse("13:00 PM")).isNull();
        assertThat(Slots.parse("Timeslot")).isNull();
        assertThat(Slots.addMinutes("23:30", 60)).isEqualTo("23:59");
        assertThat(new Slots.Slot("13:00", "13:00", "14:00").label()).isEqualTo("1:00 PM – 2:00 PM");
        assertThat(Slots.format("00:05")).isEqualTo("12:05 AM");

        assertThat(EquipmentView.countdown(Duration.ofHours(51))).isEqualTo("2d 3h");
        assertThat(EquipmentView.countdown(Duration.ofMinutes(65))).isEqualTo("1h 5m");
        assertThat(EquipmentView.countdown(Duration.ofSeconds(249))).isEqualTo("4:09");
    }
}
