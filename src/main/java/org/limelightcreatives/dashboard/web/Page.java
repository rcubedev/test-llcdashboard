package org.limelightcreatives.dashboard.web;

import org.jspecify.annotations.Nullable;
import org.limelightcreatives.dashboard.account.Person;
import org.springframework.security.web.csrf.CsrfToken;

/**
 * What every page's layout and forms need.
 *
 * @param csrf   the token forms post back, when there is one
 * @param person who is signed in, for the top bar; nobody on pages shown before signing in
 */
public record Page(Assets assets, @Nullable CsrfToken csrf, @Nullable Person person) {

    public String asset(String path) {
        return this.assets.url(path);
    }
}
