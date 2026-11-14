package org.limelightcreatives.dashboard.account;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Keeps who's signed in up to date with auth. The old app asked auth on every page; signing in with OpenID
 * Connect only tells us someone's role and name at sign-in, and a sign-in here lasts 30 days.
 *
 * <p>Auth's access tokens last 10 minutes. When ours has run out, the next page swaps the refresh token for a
 * new one and asks auth's userinfo who this is now, so a new role (or name) shows up within 10 minutes. If auth
 * won't renew it (they signed out everywhere, or the client was removed), they're signed out here and sent to
 * sign in again. If auth can't be reached, they stay as they were and the next page tries again.
 *
 * <p>Only one page of a session renews at a time (on this instance). Two instances renewing the same session at
 * the same moment is still possible: the slower one is signed out here and goes through auth, which signs them
 * straight back in.
 */
class FreshRoles extends OncePerRequestFilter {

    /** The access token the session's person was last read with. */
    static final String CHECKED_WITH = FreshRoles.class.getName() + ".token";

    private static final Logger log = LoggerFactory.getLogger(FreshRoles.class);

    private final OAuth2AuthorizedClientManager clients;
    private final OAuth2AuthorizedClientRepository stored;
    private final RestClient http;
    private final Roles roles = new Roles();
    private final SecurityContextRepository contexts = new HttpSessionSecurityContextRepository();

    /**
     * What a session's last renewal on this instance got, until those tokens run out too.
     *
     * @param person who auth then said they are; null if userinfo didn't answer
     */
    private record Renewed(OAuth2AuthorizedClient client, OAuth2AuthenticationToken person) {}

    private static final Duration EARLY = Duration.ofSeconds(60);

    private final Map<String, Renewed> renewed = new ConcurrentHashMap<>();
    private final Object[] locks = Stream.generate(Object::new).limit(64).toArray();

    FreshRoles(OAuth2AuthorizedClientManager clients, OAuth2AuthorizedClientRepository stored, RestClient.Builder http) {
        this.clients = clients;
        this.stored = stored;
        this.http = http.build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof OAuth2AuthenticationToken signedIn
                && signedIn.getPrincipal() instanceof OidcUser user) {
            check(signedIn, user, request, response);
        }
        chain.doFilter(request, response);
    }

    private void check(OAuth2AuthenticationToken signedIn, OidcUser user, HttpServletRequest request,
                       HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        if (session == null) return;

        // (no tokens at all is a local dev sign-in, DevSignIn, or a test's: nothing to renew with)
        var existing = this.stored.<OAuth2AuthorizedClient>loadAuthorizedClient(
                signedIn.getAuthorizedClientRegistrationId(), signedIn, request);
        if (existing == null) return;
        if (!runOut(existing)) {
            // still good: only note which token this sign-in came with
            if (session.getAttribute(CHECKED_WITH) == null) {
                session.setAttribute(CHECKED_WITH, existing.getAccessToken().getTokenValue());
            }
            return;
        }

        // One renewal per session at a time. Auth replaces the refresh token each time it's used, so two pages
        // of the same session renewing at once (two tabs, a background refresh) would have the slower one
        // turned down and signed out. The slower one takes over what the faster one got instead.
        synchronized (this.locks[Math.floorMod(session.getId().hashCode(), this.locks.length)]) {
            Renewed earlier = this.renewed.get(session.getId());
            if (earlier != null && !runOut(earlier.client())) {
                this.stored.saveAuthorizedClient(earlier.client(), signedIn, request, response);
                if (earlier.person() != null) {
                    signInAs(earlier.person(), request, response);
                    session.setAttribute(CHECKED_WITH, earlier.client().getAccessToken().getTokenValue());
                }
                return;
            }
            renew(signedIn, user, session, request, response);
        }
    }

    private void renew(OAuth2AuthenticationToken signedIn, OidcUser user, HttpSession session,
                       HttpServletRequest request, HttpServletResponse response) {
        OAuth2AuthorizedClient client;
        try {
            // swaps the refresh token for a new access token
            client = this.clients.authorize(OAuth2AuthorizeRequest
                    .withClientRegistrationId(signedIn.getAuthorizedClientRegistrationId())
                    .principal(signedIn)
                    .attribute(HttpServletRequest.class.getName(), request)
                    .attribute(HttpServletResponse.class.getName(), response)
                    .build());
        } catch (ClientAuthorizationException e) {
            log.info("Auth wouldn't renew {}'s sign-in ({}), signing them out", user.getSubject(), e.getError().getErrorCode());
            signOut(session);
            return;
        } catch (RuntimeException e) {
            log.warn("Couldn't reach auth to renew a sign-in, keeping it as it is: {}", e.toString());
            return;
        }
        if (client == null) return;
        // until userinfo answers, others of this session at least get the new tokens
        remember(session, client, null);

        String token = client.getAccessToken().getTokenValue();
        if (token.equals(session.getAttribute(CHECKED_WITH))) return;

        Map<String, Object> claims;
        try {
            claims = this.http.get()
                    .uri(client.getClientRegistration().getProviderDetails().getUserInfoEndpoint().getUri())
                    .headers(headers -> headers.setBearerAuth(token))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RuntimeException e) {
            log.warn("Couldn't ask auth who {} is now, keeping them as they were: {}", user.getSubject(), e.toString());
            return;
        }
        if (claims == null || !user.getSubject().equals(claims.get("sub"))) {
            log.warn("Auth's userinfo wasn't for {}, signing them out", user.getSubject());
            signOut(session);
            return;
        }

        // the ID token from sign-in, with what auth says now on top (Spring reads claims from the ID token first)
        Map<String, Object> now = new HashMap<>(user.getIdToken().getClaims());
        now.putAll(claims);
        now.put("iat", user.getIdToken().getIssuedAt());
        now.put("exp", user.getIdToken().getExpiresAt());
        var idToken = new OidcIdToken(user.getIdToken().getTokenValue(), user.getIdToken().getIssuedAt(),
                user.getIdToken().getExpiresAt(), now);
        var userInfo = new OidcUserInfo(claims);
        Collection<? extends GrantedAuthority> authorities =
                this.roles.mapAuthorities(List.of(new OidcUserAuthority(idToken, userInfo)));
        var fresh = new DefaultOidcUser(authorities, idToken, userInfo);
        var person = new OAuth2AuthenticationToken(fresh, authorities, signedIn.getAuthorizedClientRegistrationId());
        signInAs(person, request, response);
        session.setAttribute(CHECKED_WITH, token);
        remember(session, client, person);
    }

    private void signInAs(OAuth2AuthenticationToken person, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(person);
        SecurityContextHolder.setContext(context);
        this.contexts.saveContext(context, request, response);
    }

    /** Run out, or about to (Spring renews a minute early too). */
    private static boolean runOut(OAuth2AuthorizedClient client) {
        Instant expires = client.getAccessToken().getExpiresAt();
        return expires != null && Instant.now().plus(EARLY).isAfter(expires);
    }

    private void remember(HttpSession session, OAuth2AuthorizedClient client, OAuth2AuthenticationToken person) {
        this.renewed.values().removeIf(old -> runOut(old.client()));
        this.renewed.put(session.getId(), new Renewed(client, person));
    }

    private static void signOut(HttpSession session) {
        SecurityContextHolder.clearContext();
        session.invalidate();
    }
}
