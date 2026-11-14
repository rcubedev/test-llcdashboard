package org.limelightcreatives.dashboard.account;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/** Turns auth's {@code role} claim into Spring roles: {@code ROLE_ADMIN}, {@code ROLE_STAFF} or {@code ROLE_PARTICIPANT}. */
class Roles implements GrantedAuthoritiesMapper {

    @Override
    public Collection<? extends GrantedAuthority> mapAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<GrantedAuthority> mapped = new LinkedHashSet<>(authorities);
        for (GrantedAuthority authority : authorities) {
            if (authority instanceof OidcUserAuthority oidc) {
                Object role = oidc.getAttributes().get(Person.ROLE_CLAIM);
                if (role instanceof String name && !name.isBlank()) mapped.add(new SimpleGrantedAuthority("ROLE_" + name));
            }
        }
        return mapped;
    }
}
