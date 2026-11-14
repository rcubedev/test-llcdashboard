package org.limelightcreatives.dashboard.checkin;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.limelightcreatives.dashboard.checkin.Participants.Input;
import org.limelightcreatives.dashboard.checkin.Participants.Status;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ParticipantsTest extends DatabaseTest {

    @Autowired Participants participants;
    @Autowired CheckIns checkIns;

    @Test
    void everyoneWithTheirLatestCheckIn() {
        checkIns.scan("LC-001");
        checkIns.scan("LC-002");
        clock.set("2026-10-24T15:00");
        checkIns.scan("LC-002");

        assertThat(participants.list()).extracting(Participants.Participant::name, Participants.Participant::status,
                        Participants.Participant::lastAt)
                .containsExactly(
                        tuple("Sam Lee", Status.IN, "11:30:00 24/10/26"),
                        tuple("Alex Kim", Status.OUT, "15:00:00 24/10/26"),
                        tuple("Jordan Wu", Status.NONE, ""));
    }

    @Test
    void addingSomeoneTheScannerKnowsStraightAway() {
        assertThat(checkIns.scan("LC-004").status()).isEqualTo("unknown"); // and the attendee list is now cached
        participants.save(new Input("", " Riley Ng ", "riley@example.com", "0400 000 004", "", "LC-004"));
        assertThat(sheets.attendees().rows("Attendees").getLast())
                .containsExactly("Riley Ng", "riley@example.com", "0400 000 004", "", "LC-004");
        assertThat(checkIns.scan("LC-004").name()).isEqualTo("Riley Ng");
    }

    @Test
    void editingKeepsTheirRowAndCanChangeTheirId() {
        participants.save(new Input("lc-002", "Alex Kim-Lee", "alex@example.com", "0400", "000", "LC-020"));
        assertThat(sheets.attendees().rows("Attendees").get(2)).containsExactly("Alex Kim-Lee", "alex@example.com", "0400", "000", "LC-020");
        assertThat(sheets.attendees().rows("Attendees")).hasSize(4);
    }

    @Test
    void theOldRules() {
        assertThatThrownBy(() -> participants.save(new Input("", "", "", "", "", "X"))).hasMessage("Name is required.");
        assertThatThrownBy(() -> participants.save(new Input("", "A", "", "", "", " "))).hasMessage("Registration ID is required.");
        assertThatThrownBy(() -> participants.save(new Input("", "A", "", "", "", "x".repeat(101))))
                .hasMessage("Registration ID is too long.");
        assertThatThrownBy(() -> participants.save(new Input("", "A", "", "", "", "lc-001")))
                .hasMessage("Registration ID \"lc-001\" is already in use.");
        assertThatThrownBy(() -> participants.save(new Input("LC-404", "A", "", "", "", "LC-404")))
                .hasMessage("That participant no longer exists. Refresh the list.");
    }

    @Test
    void deleting() {
        participants.delete("LC-002");
        assertThat(participants.list()).extracting(Participants.Participant::registrationId).containsExactly("LC-001", "LC-003");
        assertThat(checkIns.scan("LC-002").status()).isEqualTo("unknown");
    }

    @Nested
    @AutoConfigureMockMvc
    class Pages {

        @Autowired MockMvc mvc;

        private Visitor staff() {
            return new Visitor(mvc).as(SignedIn.staff("ontik"));
        }

        @Test
        void theListWithFiltersAndSearch() throws Exception {
            checkIns.scan("LC-001");
            var staff = staff();
            assertThat(staff.get("/staff/participants").getContentAsString()).contains("All · 3", "In · 1", "Out · 0",
                    "Not arrived · 2", "Sam Lee", "ID: LC-001 · Checked in 11:30:00 24/10/26", "0400 000 101",
                    ">Check out</button>", ">Check in</button>", "href=\"/staff/participants/edit?id=LC-001\"");
            assertThat(staff.get("/staff/participants?filter=none").getContentAsString())
                    .contains("Alex Kim").doesNotContain("ID: LC-001");
            assertThat(staff.get("/staff/participants?q=jordan@").getContentAsString())
                    .contains("Jordan Wu").doesNotContain("Alex Kim");
        }

        @Test
        void checkingInByHandIsAScan() throws Exception {
            var back = staff().post("/staff/participants/toggle", "id", "LC-003", "filter", "none", "q", "");
            assertThat(back.getRedirectedUrl()).isEqualTo("/staff/participants?filter=none&q=");
            assertThat(sheets.attendees().rows("Check-ins").getLast()).containsExactly("Jordan Wu", "CHECK IN", "11:30:00 24/10/26", "LC-003");
        }

        @Test
        void addEditDelete() throws Exception {
            var staff = staff();
            assertThat(staff.get("/staff/participants/new").getContentAsString()).contains("New participant").doesNotContain(">Delete</button>");

            var refused = staff.follow(staff.post("/staff/participants", "name", "Riley", "registrationId", "LC-001"));
            assertThat(refused.getContentAsString()).contains("Registration ID \"LC-001\" is already in use.", "value=\"Riley\"");

            staff.post("/staff/participants", "name", "Riley", "registrationId", "LC 004 / A");
            String edit = staff.get("/staff/participants/edit?id=LC%20004%20%2F%20A").getContentAsString();
            assertThat(edit).contains("Edit participant", "value=\"Riley\"", "data-confirm=\"Delete “Riley” from the sheet?");

            staff.post("/staff/participants/delete", "id", "LC 004 / A");
            assertThat(participants.list()).hasSize(3);
            assertThat(staff.get("/staff/participants/edit?id=nobody").getRedirectedUrl()).isEqualTo("/staff/participants");
        }

        @Test
        void whenTheSheetIsntSetUp() throws Exception {
            sheets.attendees().tab("Attendees", List.of()); // still a tab, just empty
            assertThat(staff().get("/staff/participants").getContentAsString()).contains("No participants yet.");
        }
    }
}
