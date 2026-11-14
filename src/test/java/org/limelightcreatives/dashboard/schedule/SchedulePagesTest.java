package org.limelightcreatives.dashboard.schedule;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

/** The schedule pages; the clock is 11:30 am on Saturday 24 October, during Genesis. */
@AutoConfigureMockMvc
class SchedulePagesTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    @Test
    void participantsSeeTodayWithNowAndNext() throws Exception {
        String page = new Visitor(mvc).as(SignedIn.participant("sam")).get("/participant/schedule").getContentAsString();
        assertThat(page).contains("Saturday · 24 Oct", ">Now<", "Next · ", "📍");
        String sunday = new Visitor(mvc).as(SignedIn.participant("sam")).get("/participant/schedule?day=sun").getContentAsString();
        assertThat(sunday).doesNotContain(">Now<");
    }

    @Test
    void staffPickWhoTheyAreAndItsRemembered() throws Exception {
        var staff = new Visitor(mvc).as(SignedIn.staff("s1"));
        assertThat(staff.get("/staff/schedule").getContentAsString()).contains("Who are you?", ">Ontik<", ">Participant<");
        String mine = staff.get("/staff/schedule?person=ontik").getContentAsString();
        assertThat(mine).contains("Ontik’s Saturday", ">Now<", "Switch");
        assertThat(staff.get("/staff/schedule").getContentAsString()).contains("Ontik’s Saturday"); // remembered
        assertThat(staff.get("/staff/schedule?switchPerson=1").getContentAsString()).contains("Who are you?");
        assertThat(staff.get("/staff/schedule").getContentAsString()).contains("Who are you?");
    }

    @Test
    void theFullViewAndTryingOutAnotherTime() throws Exception {
        var staff = new Visitor(mvc).as(SignedIn.staff("s1"));
        String full = staff.get("/staff/schedule?view=full&day=sat").getContentAsString();
        assertThat(full).contains("Not on site", ">Arda<", "Participants");
        String early = staff.get("/staff/schedule?person=ontik&now=2026-10-24T06:00").getContentAsString();
        assertThat(early).contains("Your day starts 7:30am");
        String absent = staff.get("/staff/schedule?person=arda&day=sat").getContentAsString();
        assertThat(absent).contains("Not on site", "You’re not scheduled for Saturday.");
    }

    @Test
    void mistypedAddressesStillShowThePage() throws Exception {
        var staff = new Visitor(mvc).as(SignedIn.staff("s1"));
        for (String query : new String[]{"?now=lunchtime", "?now=2026-10-24Tabc", "?day=someday", "?person=nobody", "?view=sideways"}) {
            assertThat(staff.get("/staff/schedule" + query).getStatus()).as(query).isEqualTo(200);
            assertThat(staff.get("/participant/schedule" + query).getStatus()).as(query).isEqualTo(200);
        }
    }
}
