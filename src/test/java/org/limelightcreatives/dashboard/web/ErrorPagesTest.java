package org.limelightcreatives.dashboard.web;

import jakarta.servlet.RequestDispatcher;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@AutoConfigureMockMvc
class ErrorPagesTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    @Test
    void unknownPagesSayTheyDontExist() throws Exception {
        var visitor = new Visitor(mvc).as(SignedIn.participant("u1"));
        var response = visitor.get("/no-such-page");
        assertThat(response.getStatus()).isEqualTo(404);
        // MockMvc doesn't forward to /error, so render it as the server would
        var page = visitor.send(get("/error").accept(MediaType.TEXT_HTML).requestAttr(RequestDispatcher.ERROR_STATUS_CODE, 404));
        assertThat(page.getContentAsString()).contains("<title>Not found - Limelight Creatives</title>", "Not found", "never made it to the final edit", "BACK TO HOME &gt;", "Sam Lee");
    }

    @Test
    void aFailedSignInOffersToTryAgain() throws Exception {
        var response = new Visitor(mvc).get("/sign-in-failed");
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getContentAsString()).contains("We couldn’t sign you in", "/oauth2/authorization/limelight");
    }
}
