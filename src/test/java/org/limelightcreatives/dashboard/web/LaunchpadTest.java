package org.limelightcreatives.dashboard.web;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
class LaunchpadTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    @Test
    void homeIsYourLaunchpad() throws Exception {
        assertThat(new Visitor(mvc).as(SignedIn.participant("u1")).get("/").getRedirectedUrl()).isEqualTo("/participant");
        assertThat(new Visitor(mvc).as(SignedIn.staff("u2")).get("/").getRedirectedUrl()).isEqualTo("/staff");
        assertThat(new Visitor(mvc).as(SignedIn.as("u3", "ADMIN")).get("/").getRedirectedUrl()).isEqualTo("/staff");
    }

    @Test
    void participantsGetTheirApps() throws Exception {
        String page = new Visitor(mvc).as(SignedIn.participant("u1")).get("/participant").getContentAsString();
        assertThat(page).contains("Welcome!", "Limelight GENESIS", "href=\"/participant/genesis\"", "href=\"/participant/schedule\"",
                "Sam Lee", "u1@example.com", "Log out");
        assertThat(page).doesNotContain("/staff/checkin");
    }

    @Test
    void staffGetTheirs() throws Exception {
        String page = new Visitor(mvc).as(SignedIn.staff("u2")).get("/staff").getContentAsString();
        assertThat(page).contains("href=\"/staff/schedule\"", "href=\"/staff/checkin\"", "href=\"/staff/milestones\"",
                "href=\"/staff/participants\"", "href=\"/staff/resources\"", "/apps/onboard.svg?v=");
    }

    @Test
    void appsThatArentReadyYetSaySo() throws Exception {
        assertThat(new Visitor(mvc).as(SignedIn.staff("u2")).get("/staff/theatre").getContentAsString())
                .contains("This app isn’t available");
    }
}
