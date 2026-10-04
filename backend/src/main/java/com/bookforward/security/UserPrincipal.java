package com.bookforward.security;

import com.bookforward.entity.Role;
import java.security.Principal;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/** Authenticated caller. getName() is the user id so STOMP user destinations resolve per user. */
public class UserPrincipal implements Principal {
    private final UUID id;
    private final String email;
    private final Set<Role> roles;

    public UserPrincipal(UUID id, String email, Set<Role> roles) {
        this.id = id;
        this.email = email;
        this.roles = Set.copyOf(roles);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public Set<Role> getRoles() { return roles; }
    public boolean hasRole(Role r) { return roles.contains(r); }
    public boolean isStaff() { return roles.contains(Role.MODERATOR) || roles.contains(Role.ADMIN); }

    public Collection<GrantedAuthority> authorities() {
        return roles.stream().map(r -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + r.name())).toList();
    }

    @Override
    public String getName() { return id.toString(); }
}
