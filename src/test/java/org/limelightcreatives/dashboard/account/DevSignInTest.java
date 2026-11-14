package org.limelightcreatives.dashboard.account;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/** DEV_ROLE skips signing in, but only when running locally (the dev profile). */
class DevSignInTest {

    @Nested
    @AutoConfigureMockMvc
    @ActiveProfiles({"test", "dev"})
    @TestPropertySource(properties = "dashboard.dev-role=STAFF")
    class Locally extends DatabaseTest {

        @Autowired MockMvc mvc;

        @Test
        void everyoneIsThePretendUser() throws Exception {
            assertThat(mvc.perform(get("/staff")).andReturn().getResponse().getRedirectedUrl()).isNull();
        }

        @Test
        void theirSessionIsKept() throws Exception {
            // (they have no tokens from auth to renew, which mustn't sign them out)
            var browser = new Visitor(mvc);
            browser.get("/participant");
            String session = browser.session.getValue();
            browser.get("/participant");
            assertThat(browser.session.getValue()).isEqualTo(session);
        }
    }

    @Nested
    @AutoConfigureMockMvc
    @TestPropertySource(properties = "dashboard.dev-role=STAFF")
    class InProduction extends DatabaseTest {

        @Autowired MockMvc mvc;

        @Test
        void theSettingDoesNothing() throws Exception {
            assertThat(mvc.perform(get("/staff")).andReturn().getResponse().getRedirectedUrl())
                    .endsWith("/oauth2/authorization/limelight");
        }
    }
}
