package org.limelightcreatives.dashboard.account;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RolesTest {

    @Test
    void theRoleClaimBecomesASpringRole() {
        var token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "u1", "role", "STAFF"));
        var mapped = new Roles().mapAuthorities(List.of(new OidcUserAuthority(token)));
        assertThat(mapped).extracting(GrantedAuthority::getAuthority).containsExactlyInAnyOrder("OIDC_USER", "ROLE_STAFF");
    }

    @Test
    void noRoleNoSpringRole() {
        var token = new OidcIdToken("token", Instant.now(), Instant.now().plusSeconds(60), Map.of("sub", "u1"));
        assertThat(new Roles().mapAuthorities(List.of(new OidcUserAuthority(token))))
                .extracting(GrantedAuthority::getAuthority).containsExactly("OIDC_USER");
    }
}
