package org.limelightcreatives.dashboard.account;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.SignedIn;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/** Signing in is auth's job: what the dashboard sends people there with, and who may see what afterwards. */
@AutoConfigureMockMvc
class SignInTest extends DatabaseTest {

    @Autowired MockMvc mvc;

    @Test
    void anyPageWithoutASessionGoesToAuth() throws Exception {
        var session = new MockHttpSession();
        String start = mvc.perform(get("/participant/genesis").session(session)).andReturn().getResponse().getRedirectedUrl();
        assertThat(start).endsWith("/oauth2/authorization/limelight");

        String authorize = mvc.perform(get(start).session(session)).andReturn().getResponse().getRedirectedUrl();
        var query = UriComponentsBuilder.fromUriString(authorize).build().getQueryParams();
        assertThat(authorize).startsWith("https://auth.limelightcreatives.org/oauth2/authorize?");
        assertThat(query.getFirst("client_id")).isEqualTo("app_test");
        assertThat(query.getFirst("response_type")).isEqualTo("code");
        assertThat(query.getFirst("scope")).isEqualTo("openid%20profile%20email");
        assertThat(query.getFirst("redirect_uri"))
                .isEqualTo("https://dashboard.limelightcreatives.org/login/oauth2/code/limelight");
        // auth refuses apps without PKCE
        assertThat(query.getFirst("code_challenge")).isNotBlank();
        assertThat(query.getFirst("code_challenge_method")).isEqualTo("S256");
        assertThat(query.getFirst("nonce")).isNotBlank();
    }

    @Test
    void staffPagesAreForStaff() throws Exception {
        assertThat(mvc.perform(get("/staff").with(SignedIn.participant("u1"))).andReturn().getResponse().getRedirectedUrl())
                .isEqualTo("/participant");
        assertThat(mvc.perform(get("/staff/milestones").with(SignedIn.participant("u1"))).andReturn().getResponse().getRedirectedUrl())
                .isEqualTo("/participant");
        assertThat(mvc.perform(get("/staff").with(SignedIn.staff("u2"))).andReturn().getResponse().getRedirectedUrl())
                .isNull();
        assertThat(mvc.perform(get("/staff").with(SignedIn.as("u3", "ADMIN"))).andReturn().getResponse().getRedirectedUrl())
                .isNull();
    }

    @Test
    void signingOutSignsOutOfAuthToo() throws Exception {
        String location = mvc.perform(post("/logout").with(SignedIn.participant("u1")).with(csrf()))
                .andReturn().getResponse().getRedirectedUrl();
        var query = UriComponentsBuilder.fromUriString(location).build().getQueryParams();
        assertThat(location).startsWith("https://auth.limelightcreatives.org/connect/logout?");
        assertThat(query.getFirst("id_token_hint")).isNotBlank();
        assertThat(query.getFirst("post_logout_redirect_uri")).isEqualTo("https://dashboard.limelightcreatives.org/");
    }

    @Test
    void formsNeedTheirCsrfToken() throws Exception {
        assertThat(mvc.perform(post("/logout").with(SignedIn.participant("u1"))).andReturn().getResponse().getStatus())
                .isEqualTo(403);
    }
}
