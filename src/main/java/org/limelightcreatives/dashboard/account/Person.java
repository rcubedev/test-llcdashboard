package org.limelightcreatives.dashboard.account;

import org.springframework.security.oauth2.core.oidc.StandardClaimNames;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Who is signed in, from the claims auth sends.
 *
 * @param id    the user's id in auth ({@code sub}, a text UUID), which teams and submissions store
 * @param email their email
 * @param name  their full name, e.g. "Sam Lee"
 * @param role  ADMIN, STAFF or PARTICIPANT
 */
public record Person(String id, String email, String name, String role) {

    /** The claim auth puts the role in. */
    public static final String ROLE_CLAIM = "role";

    public static Person of(OidcUser user) {
        return new Person(user.getSubject(), user.getEmail(), user.getClaimAsString(StandardClaimNames.NAME),
                user.getClaimAsString(ROLE_CLAIM));
    }

    public boolean isStaff() {
        return "ADMIN".equals(role) || "STAFF".equals(role);
    }

    /** Up to two initials for the avatar, e.g. "SL" for Sam Lee. */
    public String initials() {
        if (name == null || name.isBlank()) return "?";
        var initials = new StringBuilder();
        for (String part : name.strip().split("\\s+")) {
            if (initials.length() == 2) break;
            initials.appendCodePoint(part.codePointAt(0));
        }
        return initials.toString().toUpperCase();
    }

    public String displayName() {
        return name == null || name.isBlank() ? "User Unavailable" : name;
    }
}
