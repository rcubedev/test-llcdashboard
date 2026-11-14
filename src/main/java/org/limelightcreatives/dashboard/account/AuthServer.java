package org.limelightcreatives.dashboard.account;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcScopes;

import java.util.Map;

/**
 * The auth app, which signs everyone in (OpenID Connect, authorization code with PKCE). Its endpoints are
 * written out rather than discovered so the dashboard starts (and its tests run) without reaching it.
 *
 * <p>The dashboard is an app on auth's apps page, which gives it its client id and secret. With
 * {@code DEV_ROLE} nobody signs in through auth, so it starts without them.
 */
@Configuration
public class AuthServer {

    /** The registration's id, also in this app's callback address: /login/oauth2/code/limelight. */
    public static final String REGISTRATION = "limelight";

    @Bean
    ClientRegistrationRepository clientRegistrations(DashboardProperties properties,
                                                     @Value("${dashboard.dev-role:}") String devRole) {
        var auth = properties.auth();
        if (isBlank(auth.clientId()) || isBlank(auth.clientSecret())) {
            if (!devRole.isBlank()) return registrationId -> null;
            String base = properties.baseUrl().toString().replaceFirst("/+$", "");
            throw new IllegalStateException("Set AUTH_CLIENT_ID and AUTH_CLIENT_SECRET, or nobody can sign in. Add the "
                    + "dashboard on " + issuer(properties) + "/apps as a server app with the redirect URL " + base
                    + "/login/oauth2/code/" + REGISTRATION + ", the sign-out redirect URL " + base
                    + "/ and RS256 ID tokens, and it shows both");
        }
        return new InMemoryClientRegistrationRepository(registration(properties));
    }

    static ClientRegistration registration(DashboardProperties properties) {
        var auth = properties.auth();
        String issuer = issuer(properties);
        return ClientRegistration.withRegistrationId(REGISTRATION)
                .clientName("Limelight Creatives")
                .clientId(auth.clientId())
                .clientSecret(auth.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                // auth only accepts the addresses it has registered, so this is the configured one, not the request's
                .redirectUri(properties.baseUrl().toString().replaceFirst("/+$", "") + "/login/oauth2/code/" + REGISTRATION)
                .scope(OidcScopes.OPENID, OidcScopes.PROFILE, OidcScopes.EMAIL)
                .issuerUri(issuer)
                .authorizationUri(issuer + "/oauth2/authorize")
                .tokenUri(issuer + "/oauth2/token")
                .jwkSetUri(issuer + "/oauth2/jwks")
                .userInfoUri(issuer + "/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(Map.of("end_session_endpoint", issuer + "/connect/logout"))
                // auth requires PKCE from every app, Spring only sends it for public clients unless asked
                .clientSettings(ClientRegistration.ClientSettings.builder().requireProofKey(true).build())
                .build();
    }

    private static String issuer(DashboardProperties properties) {
        return properties.auth().url().toString().replaceFirst("/+$", "");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
