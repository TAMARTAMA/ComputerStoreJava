package com.tamar.computerstore.security;

import com.tamar.computerstore.entity.UserAccount;
import com.tamar.computerstore.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal stored in the SecurityContext. Carries the account id and role so downstream
 * code can authorise without another database round trip.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final UserRole role;
    private final boolean enabled;

    public AuthenticatedUser(UserAccount account) {
        this.id = account.getId();
        this.email = account.getEmail();
        this.passwordHash = account.getPasswordHash();
        this.role = account.getRole();
        this.enabled = account.isEnabled();
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
