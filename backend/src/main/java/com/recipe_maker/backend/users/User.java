package com.recipe_maker.backend.users;

import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.recipe_maker.backend.roles.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity class representing a user in the system.
 */
@AllArgsConstructor
@Data
@Entity
@NoArgsConstructor
@Table(name = "users")
public class User implements UserDetails {
    /** The unique identifier for the user. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The username for the user. */
    @Column(unique = true, nullable = false, updatable = false)
    private String username;

    /** The password for the user. */
    @Column(nullable = false)
    private String password;

    /** The email for the user. */
    @Column(unique = true, nullable = false)
    private String email;

    /** Enabled status for user account. */
    @Column(nullable = false)
    private boolean enabled = true;

    /** Locked status for the user account. */
    @Column(nullable = false)
    private boolean accountNonLocked = true;

    /** Value tracking number of consecutive failed login attempts.  */
    @Column(nullable = false)
    private int failedLoginAttempts = 0;

    /** Time when the account will be unlocked. */
    @Column
    private Instant lockedUntil;

    /** The roles of the user. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Return granted authorities for user based on the stored Role objects.
     * 
     * @return Collection of GrantedAuthority objects
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.getRoles()
            .stream()
            .map(role -> new SimpleGrantedAuthority(role.getName().name()))
            .collect(Collectors.toSet());
    }

    /**
     * Determine if the user account is enabled.
     * 
     * @return boolean true if the account is enabled, false if it is disabled
     */
    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    /**
     * Check if the account is locked, taking into account the account lock expiration time.
     * 
     * @return boolean true if the account is not locked, false if it is 
     */
    @Override
    public boolean isAccountNonLocked() {
        if (lockedUntil != null && Instant.now().isAfter(lockedUntil)) {
            return true;
        }

        return accountNonLocked;
    }
}
