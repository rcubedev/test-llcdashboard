package org.limelightcreatives.dashboard;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;

/**
 * Settings under {@code dashboard:} in application.yml, mostly filled from environment variables (the same
 * names as the old app's).
 *
 * @param baseUrl            this app's public address, e.g. https://dashboard.limelightcreatives.org
 * @param auth               the auth app, which people sign in through
 * @param teamsLocked        stop people creating, joining or leaving teams
 * @param submissionsCloseAt after this teams can't change their project, e.g. 2026-11-01T23:59:00+11:00;
 *                           empty for never
 * @param fillout            the forms teams submit milestones with
 * @param attendeeSheet      the Google Sheet with participants (tab Attendees) and check-ins (tab Check-ins)
 * @param equipmentSheet     the Google Sheet with the equipment booking grid
 */
@ConfigurationProperties("dashboard")
public record DashboardProperties(
        URI baseUrl,
        Auth auth,
        boolean teamsLocked,
        OffsetDateTime submissionsCloseAt,
        Fillout fillout,
        Sheet attendeeSheet,
        Sheet equipmentSheet
) {

    public DashboardProperties {
        if (fillout == null) fillout = new Fillout(null, null);
        if (attendeeSheet == null) attendeeSheet = new Sheet(null, null, null, null);
        if (equipmentSheet == null) equipmentSheet = new Sheet(null, null, null, null);
    }

    /**
     * A Google Sheet, read and written as a service account (share the sheet with its email as an Editor).
     *
     * @param privateKey the service account's private key: the PEM text (literal \n is fine) or the PEM in base64
     * @param tab        which tab to use, where that's a choice; empty for the default
     */
    public record Sheet(String serviceAccountEmail, String privateKey, String sheetId, String tab) {

        public boolean configured() {
            return notBlank(serviceAccountEmail) && notBlank(privateKey) && notBlank(sheetId);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }

    /**
     * Fillout, where teams submit milestones.
     *
     * @param webhookSecret what Fillout's webhook sends as {@code ?secret=}; without one every call is refused
     * @param resultsUrl    the form's results page, linked from the staff milestones page (optional)
     */
    public record Fillout(String webhookSecret, String resultsUrl) {}

    /**
     * The auth app and this app's registration there, from auth's apps page.
     *
     * @param url          its address, which is also its OpenID Connect issuer, e.g. https://auth.limelightcreatives.org
     * @param clientId     this app's client id there
     * @param clientSecret this app's client secret there
     */
    public record Auth(URI url, String clientId, String clientSecret) {}

    public boolean submissionsClosed(Instant now) {
        return submissionsCloseAt != null && now.isAfter(submissionsCloseAt.toInstant());
    }
}
