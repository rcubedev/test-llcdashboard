package org.limelightcreatives.dashboard.account;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Locally only: {@code DEV_ROLE=STAFF ./gradlew bootTestRun} (or PARTICIPANT) makes everyone a pretend
 * "Development User" with that role, so the dashboard can be tried without running the auth app (the old app's
 * DEV_BYPASS_AUTH). It needs the dev profile as well as the setting, so production can't turn it on by accident.
 */
@Component
@Profile("dev")
@ConditionalOnExpression("!'${dashboard.dev-role:}'.isBlank()")
public class DevSignIn extends OncePerRequestFilter {

    private final OAuth2AuthenticationToken authentication;

    public DevSignIn(@Value("${dashboard.dev-role}") String role) {
        var token = new OidcIdToken("dev", Instant.EPOCH, Instant.now().plusSeconds(365L * 24 * 3600), Map.of(
                "sub", "0",
                "email", "supercooldeveloper@limelightcreatives.org",
                "name", "Development User",
                Person.ROLE_CLAIM, role));
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role), new SimpleGrantedAuthority("OIDC_USER"));
        this.authentication = new OAuth2AuthenticationToken(new DefaultOidcUser(authorities, token), authorities,
                AuthServer.REGISTRATION);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        var context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null) context.setAuthentication(this.authentication);
        chain.doFilter(request, response);
    }
}
