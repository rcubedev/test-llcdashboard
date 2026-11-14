package org.limelightcreatives.dashboard.account;

import org.junit.jupiter.api.Test;
import org.limelightcreatives.dashboard.DashboardProperties;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/** The dashboard's client id and secret come from auth's apps page. */
class AuthServerTest {

    @Test
    void withoutThemItWontStartAndSaysWhatToRegister() {
        assertThatIllegalStateException()
                .isThrownBy(() -> new AuthServer().clientRegistrations(properties("app_x", " "), ""))
                .withMessageContaining("AUTH_CLIENT_ID and AUTH_CLIENT_SECRET")
                .withMessageContaining("http://localhost:8080/apps")
                .withMessageContaining("redirect URL http://localhost:3000/login/oauth2/code/limelight,")
                .withMessageContaining("sign-out redirect URL http://localhost:3000/ ")
                .withMessageContaining("RS256");
    }

    @Test
    void aDevRoleStartsWithoutThem() {
        var registrations = new AuthServer().clientRegistrations(properties(null, null), "STAFF");
        assertThat(registrations.findByRegistrationId(AuthServer.REGISTRATION)).isNull();
    }

    private static DashboardProperties properties(String clientId, String clientSecret) {
        return new DashboardProperties(URI.create("http://localhost:3000/"),
                new DashboardProperties.Auth(URI.create("http://localhost:8080"), clientId, clientSecret),
                false, null, null, null, null);
    }
}
