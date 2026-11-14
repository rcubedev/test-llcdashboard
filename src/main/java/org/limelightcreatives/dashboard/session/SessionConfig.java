package org.limelightcreatives.dashboard.session;

import org.limelightcreatives.dashboard.DashboardProperties;
import org.springframework.boot.session.autoconfigure.SessionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.jdbc.PostgreSqlJdbcIndexedSessionRepositoryCustomizer;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * Sessions live in PostgreSQL (Spring Session), so any instance can serve any visitor and a redeploy signs
 * nobody out.
 *
 * <p>The cookie is the dashboard's own, on this host only: signing in is auth's job, and auth's cookie stays
 * auth's.
 */
@Configuration
class SessionConfig {

    static final String COOKIE = "dashboard-session";

    @Bean
    CookieSerializer cookieSerializer(SessionProperties sessions, DashboardProperties properties) {
        var cookie = new DefaultCookieSerializer();
        cookie.setCookieName(COOKIE);
        cookie.setCookiePath("/");
        cookie.setUseHttpOnlyCookie(true);
        // Secure whenever the dashboard's own address is https, whatever the host's proxy says about the request
        cookie.setUseSecureCookie("https".equalsIgnoreCase(properties.baseUrl().getScheme()));
        cookie.setSameSite("Lax");
        // the browser keeps it as long as a session can last, the database decides whether it still does
        cookie.setCookieMaxAge((int) sessions.getTimeout().toSeconds());
        return cookie;
    }

    /** Saves attributes with INSERT ... ON CONFLICT, so two requests at once can't clash. */
    @Bean
    PostgreSqlJdbcIndexedSessionRepositoryCustomizer postgresSessions() {
        return new PostgreSqlJdbcIndexedSessionRepositoryCustomizer();
    }
}
