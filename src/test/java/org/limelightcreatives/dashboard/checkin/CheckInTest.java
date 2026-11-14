package org.limelightcreatives.dashboard.checkin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.FakeSpreadsheets;
import org.limelightcreatives.dashboard.InMemorySpreadsheet;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

class CheckInTest extends DatabaseTest {

    @Autowired CheckIns checkIns;

    @Test
    void eachScanFlipsInAndOutAndIsLogged() {
        var first = checkIns.scan(" lc-001 ");
        assertThat(first).isEqualTo(new CheckInResult("ok", "CHECK IN", "LC-001", "Sam Lee", "11:30:00 24/10/26", null));
        assertThat(first.label()).isEqualTo("Checked in");

        clock.set("2026-10-24T17:05:09");
        var second = checkIns.scan("LC-001");
        assertThat(second.action()).isEqualTo("CHECK OUT");
        assertThat(second.label()).isEqualTo("Checked out");
        assertThat(checkIns.scan("LC-001").action()).isEqualTo("CHECK IN");

        assertThat(sheets.attendees().rows("Check-ins")).containsExactly(
                List.of("Participant Name", "Action", "Timestamp", "Registration ID"),
                List.of("Sam Lee", "CHECK IN", "11:30:00 24/10/26", "LC-001"),
                List.of("Sam Lee", "CHECK OUT", "17:05:09 24/10/26", "LC-001"),
                List.of("Sam Lee", "CHECK IN", "17:05:09 24/10/26", "LC-001"));
    }

    @Test
    void everyonesStateIsTheirOwn() {
        checkIns.scan("LC-001");
        assertThat(checkIns.scan("LC-002").action()).isEqualTo("CHECK IN");
    }

    @Test
    void unknownAndBadCodes() {
        var unknown = checkIns.scan("LC-999");
        assertThat(unknown.status()).isEqualTo("unknown");
        assertThat(unknown.problem()).isEqualTo("No participant with ID \"LC-999\".");
        assertThat(checkIns.scan("  ").problem()).isEqualTo("Invalid code.");
        assertThat(checkIns.scan("x".repeat(101)).problem()).isEqualTo("Invalid code.");
        assertThat(sheets.attendees().rows("Check-ins")).hasSize(1);
    }

    @Test
    void aSheetThatDoesntAnswer() {
        var broken = new CheckIns(new FakeSpreadsheets() {
            @Override
            public InMemorySpreadsheet attendees() {
                throw new IllegalStateException("Google is down");
            }
        }, clock);
        assertThat(broken.scan("LC-001").problem()).isEqualTo("Couldn't reach the sheet. Try again.");
    }

    @Nested
    @AutoConfigureMockMvc
    class Pages {

        @Autowired MockMvc mvc;

        @Test
        void theCameraPostsCodesAsJson() throws Exception {
            var response = mvc.perform(post("/staff/checkin/scan").with(SignedIn.staff("ontik")).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"LC-002\"}"))
                    .andReturn().getResponse();
            assertThat(response.getContentAsString()).contains("\"status\":\"ok\"", "\"action\":\"CHECK IN\"",
                    "\"id\":\"LC-002\"", "\"name\":\"Alex Kim\"");
        }

        @Test
        void theCameraNeedsTheTokenFromThePage() throws Exception {
            String page = mvc.perform(get("/staff/checkin").with(SignedIn.staff("ontik"))).andReturn().getResponse().getContentAsString();
            assertThat(page).contains("name=\"csrf-token\"", "name=\"csrf-header\" content=\"X-CSRF-TOKEN\"", "/js/checkin.js?v=");

            int status = mvc.perform(post("/staff/checkin/scan").with(SignedIn.staff("ontik"))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"LC-002\"}")).andReturn().getResponse().getStatus();
            assertThat(status).isEqualTo(403);
        }

        @Test
        void typingAnIdWorksWithoutJavascript() throws Exception {
            var staff = new Visitor(mvc).as(SignedIn.staff("ontik"));
            var page = staff.follow(staff.post("/staff/checkin", "code", "LC-003"));
            assertThat(page.getContentAsString()).contains("Checked in", "Jordan Wu", "ID: LC-003 · 11:30:00 24/10/26");
            assertThat(staff.follow(staff.post("/staff/checkin", "code", "nope")).getContentAsString())
                    .contains("Unknown ID", "No participant with ID \"nope\".");
        }

        @Test
        void forStaffOnly() throws Exception {
            var sam = new Visitor(mvc).as(SignedIn.participant("sam"));
            assertThat(sam.post("/staff/checkin", "code", "LC-001").getRedirectedUrl()).isEqualTo("/participant");
            assertThat(sheets.attendees().rows("Check-ins")).hasSize(1);
        }

        @Test
        void theScannerIsServedFromItsWebJar() throws Exception {
            var response = mvc.perform(get("/webjars/qr-scanner/qr-scanner.min.js").with(SignedIn.staff("ontik")))
                    .andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).contains("export");
        }
    }
}
