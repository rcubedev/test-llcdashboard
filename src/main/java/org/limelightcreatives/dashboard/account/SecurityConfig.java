package org.limelightcreatives.dashboard.account;

import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.web.client.RestClient;
import org.limelightcreatives.dashboard.DashboardProperties;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;

/**
 * Who may see what. Everyone signs in through auth (there's no sign-in page here): opening any page without a
 * session goes straight there and comes back to that page. {@code /staff} is for ADMIN and STAFF, anyone else
 * is sent to {@code /participant}, as the old middleware did.
 *
 * <p>Forms are protected by CSRF tokens (the form tag adds them to every form). The Fillout
 * webhook is the only thing posted from elsewhere, and it checks its own secret.
 */
@Configuration
public class SecurityConfig {

    /** Paths anyone may open. */
    static final String[] PUBLIC = {
            "/error", "/sign-in-failed", "/actuator/health", "/api/fillout/webhook",
            "/css/**", "/js/**", "/fonts/**", "/branding/**", "/favicon.ico"
    };

    /** Where signing in starts: Spring sends people from here to auth. */
    static final String SIGN_IN = "/oauth2/authorization/" + AuthServer.REGISTRATION;

    /** Auth's tokens for whoever is signed in are kept in their session, so in the database like the session. */
    @Bean
    OAuth2AuthorizedClientRepository authorizedClients() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    @Bean
    SecurityFilterChain app(HttpSecurity http, ClientRegistrationRepository registrations,
                            OAuth2AuthorizedClientManager clients, OAuth2AuthorizedClientRepository authorizedClients,
                            RestClient.Builder restClient,
                            DashboardProperties properties, ObjectProvider<DevSignIn> devSignIn) throws Exception {
        var requestCache = new HttpSessionRequestCache();
        // back to the exact address after signing in, without Spring's "?continue" marker
        requestCache.setMatchingRequestParameterName(null);

        // signing out here signs out of auth too, which then sends them back to the dashboard
        var signOut = new OidcClientInitiatedLogoutSuccessHandler(registrations);
        signOut.setPostLogoutRedirectUri(properties.baseUrl().toString().replaceFirst("/+$", "") + "/");

        http
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC).permitAll()
                        .requestMatchers("/staff", "/staff/**").hasAnyRole("ADMIN", "STAFF")
                        .anyRequest().authenticated())
                .requestCache(cache -> cache.requestCache(requestCache))
                .oauth2Login(login -> login
                        .loginPage(SIGN_IN)
                        .userInfoEndpoint(userInfo -> userInfo.userAuthoritiesMapper(new Roles()))
                        .failureUrl("/sign-in-failed"))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint(SIGN_IN))
                        .accessDeniedHandler((request, response, denied) -> {
                            // a form without its token (or from a stale page) is refused, not sent anywhere
                            if (denied instanceof CsrfException) response.sendError(HttpServletResponse.SC_FORBIDDEN);
                            // signed in but not staff
                            else response.sendRedirect("/participant");
                        }))
                .logout(logout -> logout.logoutSuccessHandler(signOut))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/fillout/webhook"))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);
        http.addFilterBefore(new FreshRoles(clients, authorizedClients, restClient), AuthorizationFilter.class);
        devSignIn.ifAvailable(dev -> http.addFilterBefore(dev, AnonymousAuthenticationFilter.class));
        return http.build();
    }
}
