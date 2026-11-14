package org.limelightcreatives.dashboard.account;

import com.nimbusds.jose.JWSAlgorithm;
import org.junit.jupiter.api.AfterAll;
import jakarta.servlet.http.Cookie;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DatabaseTest;
import org.limelightcreatives.dashboard.Visitor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
/** The whole sign-in, against a pretend auth: off to auth, back with a code, swapped for a checked ID token. */
@AutoConfigureMockMvc
class SignInThroughAuthTest extends DatabaseTest {

    private static final PretendAuth AUTH = start();

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        registry.add("dashboard.auth.url", () -> AUTH.issuer);
    }

    @AfterAll
    static void stop() {
        AUTH.close();
    }

    @Autowired MockMvc mvc;

    private Visitor browser;

    @BeforeEach
    void newBrowser() {
        browser = new Visitor(mvc);
        AUTH.expiresIn = 600;
        AUTH.refuseRefresh = false;
        AUTH.idTokenAlgorithm = JWSAlgorithm.RS256;
    }

    @Test
    void aNewRoleArrivesWhenTheAccessTokenIsRenewed() throws Exception {
        AUTH.expiresIn = 30; // run out already, so every page renews it
        signIn("participant-1", "PARTICIPANT");
        assertThat(browser.get("/staff").getRedirectedUrl()).isEqualTo("/participant");

        AUTH.nowIs("STAFF", "Sam Staff"); // promoted in auth's database
        var staff = browser.get("/staff");
        assertThat(staff.getRedirectedUrl()).isNull();
        assertThat(staff.getContentAsString()).contains("Sam Staff");
        assertThat(AUTH.lastTokenRequest).containsEntry("grant_type", "refresh_token");

        AUTH.nowIs("PARTICIPANT", "Sam Staff"); // and back
        assertThat(browser.get("/staff").getRedirectedUrl()).isEqualTo("/participant");
    }

    @Test
    void withAValidAccessTokenAuthIsntAsked() throws Exception {
        signIn("participant-1", "PARTICIPANT");
        AUTH.nowIs("STAFF", "Sam Staff");
        assertThat(browser.get("/staff").getRedirectedUrl()).isEqualTo("/participant"); // until the token runs out
        assertThat(AUTH.lastTokenRequest).containsEntry("grant_type", "authorization_code");
    }

    @Test
    void pagesOpenedAtOnceRenewOnlyOnce() throws Exception {
        AUTH.expiresIn = 30; // the sign-in's token has run out already
        signIn("participant-1", "PARTICIPANT");
        AUTH.expiresIn = 600; // the renewed one lasts
        int before = AUTH.refreshes;
        Cookie session = browser.session;

        // two tabs and a few background refreshes at the same moment; auth's refresh tokens only work once,
        // so if each renewed for itself all but one would be turned down and signed out
        int pages = 8;
        var start = new CountDownLatch(1);
        List<Future<Integer>> statuses = new ArrayList<>();
        try (var pool = Executors.newFixedThreadPool(pages)) {
            for (int i = 0; i < pages; i++) {
                statuses.add(pool.submit(() -> {
                    start.await();
                    return mvc.perform(get("/participant").cookie(session)).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
        }
        for (var status : statuses) assertThat(status.get()).isEqualTo(200);
        assertThat(AUTH.refreshes - before).isEqualTo(1);
        assertThat(browser.get("/participant").getStatus()).isEqualTo(200);
    }

    @Test
    void signedOutHereWhenAuthWontRenew() throws Exception {
        AUTH.expiresIn = 30;
        signIn("participant-1", "PARTICIPANT");
        assertThat(browser.get("/participant").getStatus()).isEqualTo(200);

        AUTH.refuseRefresh = true; // e.g. they signed out everywhere
        assertThat(browser.get("/participant").getRedirectedUrl()).endsWith("/oauth2/authorization/limelight");
    }

    @Test
    void signsInAndComesBackToThePageTheyOpened() throws Exception {
        var query = signIn("staff-1", "STAFF");

        // the code, swapped server-to-server with the client secret and the PKCE verifier
        assertThat(AUTH.lastTokenAuthorization).isEqualTo(PretendAuth.basic("app_test", "test-dashboard-secret-test-dashboard"));
        assertThat(AUTH.lastTokenRequest).containsEntry("code", "the-code").containsKey("code_verifier");
        assertThat(query.getFirst("code_challenge")).isNotBlank();

        // signed in as staff from auth's role claim
        assertThat(browser.get("/staff").getRedirectedUrl()).isNull();
        assertThat(db.sql("SELECT principal_name FROM dashboard_session").query(String.class).single()).isEqualTo("staff-1");
    }

    @Test
    void anEs256IdTokenIsRefusedSoTheDashboardIsRegisteredWithRs256() throws Exception {
        AUTH.idTokenAlgorithm = JWSAlgorithm.ES256;
        String authorize = browser.follow(browser.get("/participant/genesis")).getRedirectedUrl();
        var query = UriComponentsBuilder.fromUriString(authorize).build().getQueryParams();
        AUTH.willSignIn("staff-1", "STAFF", decoded(query.getFirst("nonce")));

        var callback = browser.get("/login/oauth2/code/limelight", "code", "the-code", "state", decoded(query.getFirst("state")));
        assertThat(callback.getRedirectedUrl()).isEqualTo("/sign-in-failed");
    }

    @Test
    void participantsArentStaff() throws Exception {
        signIn("participant-1", "PARTICIPANT");
        assertThat(browser.get("/staff").getRedirectedUrl()).isEqualTo("/participant");
    }

    @Test
    void aTokenForSomeoneElsesSignInIsRefused() throws Exception {
        String authorize = browser.follow(browser.get("/participant/genesis")).getRedirectedUrl();
        var query = UriComponentsBuilder.fromUriString(authorize).build().getQueryParams();
        AUTH.willSignIn("someone", "ADMIN", "another-nonce");

        var callback = browser.get("/login/oauth2/code/limelight", "code", "the-code", "state", decoded(query.getFirst("state")));
        assertThat(callback.getRedirectedUrl()).isEqualTo("/sign-in-failed");
        assertThat(browser.get("/staff").getRedirectedUrl()).endsWith("/oauth2/authorization/limelight");
    }

    /** Opens a page, goes through the pretend auth as this person, and checks they land back on that page. */
    private MultiValueMap<String, String> signIn(String id, String role) throws Exception {
        String authorize = browser.follow(browser.get("/participant/genesis")).getRedirectedUrl();
        assertThat(authorize).startsWith(AUTH.issuer + "/oauth2/authorize?");
        var query = UriComponentsBuilder.fromUriString(authorize).build().getQueryParams();
        AUTH.willSignIn(id, role, decoded(query.getFirst("nonce")));

        var callback = browser.get("/login/oauth2/code/limelight", "code", "the-code", "state", decoded(query.getFirst("state")));
        assertThat(callback.getRedirectedUrl()).isEqualTo("https://dashboard.limelightcreatives.org/participant/genesis");
        return query;
    }

    private static String decoded(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private static PretendAuth start() {
        try {
            return new PretendAuth();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
