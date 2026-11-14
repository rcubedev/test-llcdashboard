package org.limelightcreatives.dashboard;

import org.limelightcreatives.dashboard.account.AuthServer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;

/** Someone signed in through auth, for MockMvc requests: {@code mvc.perform(get("/").with(SignedIn.as(...)))}. */
public final class SignedIn {

    private static final ClientRegistration AUTH = ClientRegistration.withRegistrationId(AuthServer.REGISTRATION)
            .clientId("app_test")
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("https://dashboard.limelightcreatives.org/login/oauth2/code/limelight")
            .authorizationUri("https://auth.limelightcreatives.org/oauth2/authorize")
            .tokenUri("https://auth.limelightcreatives.org/oauth2/token")
            .build();

    private SignedIn() {}

    public static RequestPostProcessor participant(String id) {
        return as(id, "PARTICIPANT");
    }

    public static RequestPostProcessor staff(String id) {
        return as(id, "STAFF");
    }

    /** {@code id} as auth's user id, with the name "Sam Lee" and the email "{@code id}@example.com". */
    public static RequestPostProcessor as(String id, String role) {
        return oidcLogin()
                .clientRegistration(AUTH)
                .idToken(token -> token
                        .subject(id)
                        .claim("email", id + "@example.com")
                        .claim("name", "Sam Lee")
                        .claim("role", role))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role), new SimpleGrantedAuthority("OIDC_USER"));
    }
}
