package com.recipe_maker.backend.components;

import java.time.Instant;

import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import com.recipe_maker.backend.users.UserRepository;

/**
 * Event listener that handles failed login logic and user account status.
 */
@Component
public class AuthenticationEventListener {
    /** Repository for managing user data. */
    private final UserRepository userRepository;

    /** Max number of failed login attempts before an account lock. */
    private static final int MAX_ATTEMPTS = 5;

    /** Duration of an account lock. */
    private static final int LOCK_DURATION_MINUTES = 15;

    /**
     * Constructor for AuthenticationEventListener.
     * 
     * @param userRepository
     */
    public AuthenticationEventListener(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Increment the failed attempts counter for a user, and lock them out of their account if
     * they exceed the threshhold. 
     * 
     * @param failureEvent AbstractAuthenticationFailureEvent passed after failed login
     */
    @EventListener
    public void onFailure(AbstractAuthenticationFailureEvent failureEvent) {
        // Extract the username from the event
        String username = failureEvent.getAuthentication().getName();

        // Check for the username in the database
        userRepository.findByUsername(username).ifPresent((user) -> {
            // Bump up the failed login attempts by one
            user.setFailedLoginAttempts(user.getFailedLoginAttempts() + 1);

            // If failed logins exceed threshhold
            if (user.getFailedLoginAttempts() >= MAX_ATTEMPTS) {
                // Lock account
                user.setAccountNonLocked(false);

                // Set lock expiration
                user.setLockedUntil(Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60));
            }

            // Save user data to database
            userRepository.save(user);
        });
    }

    /**
     * On successful login, reset failed login attempts and keep the user's account
     * unlocked. 
     * 
     * @param successEvent AuthenticationSuccessEvent passed after successful login
     */
    @EventListener
    public void onSuccess(AuthenticationSuccessEvent successEvent) {
        // Extract the username from the event
        String username = successEvent.getAuthentication().getName();

        // Check for the username in the database
        userRepository.findByUsername(username).ifPresent((user) -> {
            // Reset user account status values
            user.setFailedLoginAttempts(0);
            user.setAccountNonLocked(true);
            user.setLockedUntil(null);

            // Save user data to database
            userRepository.save(user);
        });
    }
}
