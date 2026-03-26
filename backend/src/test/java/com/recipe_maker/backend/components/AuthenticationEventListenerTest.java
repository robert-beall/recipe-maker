package com.recipe_maker.backend.components;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;

import com.recipe_maker.backend.users.User;
import com.recipe_maker.backend.users.UserRepository;
import com.recipe_maker.backend.users.UserTestUtils;

/**
 * Test class for AuthenticationEventListener.
 */
@ExtendWith(MockitoExtension.class)
public class AuthenticationEventListenerTest {
    /** AuthenticationEventListener to test. */
    @InjectMocks
    private AuthenticationEventListener authenticationEventListener;

    /** Mocked repository to manage user data. */
    @Mock
    private UserRepository userRepository;

    /** Mocked AbstractAuthenticationFailureEvent to pass as a parameter. */
    @Mock
    private AbstractAuthenticationFailureEvent failureEvent;

    /** Mocked AuthenticationSuccessEvent to pass as a parameter. */
    @Mock
    private AuthenticationSuccessEvent successEvent;

    /** Mocked Authentication object. */
    @Mock
    private Authentication authentication;

    /** Utils class to generate user test data. */
    private UserTestUtils userTestUtils = new UserTestUtils();

    /**
     * Test onFailure() method when the user has not met the lockout threshhold. 
     */
    @Test
    void testOnFailureNoLockout() {
        // Create a test user
        User user = userTestUtils.createEntity();

        // Mock the event and repository calls
        when(failureEvent.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // Assert that no exceptions are thrown during execution
        assertDoesNotThrow(() -> {
            // Create argument captor to retrieve entity values
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            
            // Call the method
            authenticationEventListener.onFailure(failureEvent);

            // Verify saving the user and capture the passed entity
            verify(userRepository, times(1)).save(captor.capture());

            // Extract the user entity
            User result = captor.getValue();

            // Assert expected values
            assertEquals(user.getId(), result.getId());
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getPassword(), result.getPassword());
            assertEquals(user.getEmail(), result.getEmail());
            assertEquals(user.getRoles(), result.getRoles());
            assertTrue(user.isAccountNonLocked()); // Account should not be locked
            assertEquals(1, result.getFailedLoginAttempts()); // Expect 1 failed attempt
            assertNull(result.getLockedUntil());
        });
    }

    /**
     * Test onFailure() method when the user has met the lockout threshhold and their account is
     * locked. 
     */
    @Test
    void testOnFailureWithLockout() {
        // Create a test user
        User user = userTestUtils.createEntity();
        user.setFailedLoginAttempts(4);

        // Mock the event and repository calls
        when(failureEvent.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // Assert that no exceptions are thrown during execution
        assertDoesNotThrow(() -> {
            // Create argument captor to retrieve entity values
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            // Call the method
            authenticationEventListener.onFailure(failureEvent);

            // Verify saving the user and capture the passed entity
            verify(userRepository, times(1)).save(captor.capture());

            // Extract the user entity
            User result = captor.getValue();

            // Assert expected values
            assertEquals(user.getId(), result.getId());
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getPassword(), result.getPassword());
            assertEquals(user.getEmail(), result.getEmail());
            assertEquals(user.getRoles(), result.getRoles());
            assertFalse(user.isAccountNonLocked()); // Account should be locked
            assertEquals(5, result.getFailedLoginAttempts()); // Expect 1 failed attempt
            assertNotNull(result.getLockedUntil());
        });
    }

    /**
     * Test onSuccess() method. 
     */
    @Test
    void testOnSuccess() {
        // Create a test user
        User user = userTestUtils.createEntity();

        // Mock the event and repository calls
        when(successEvent.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        // Assert that no exceptions are thrown during execution
        assertDoesNotThrow(() -> {
            // Create argument captor to retrieve entity values
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            
            // Call the method
            authenticationEventListener.onSuccess(successEvent);

            verify(userRepository, times(1)).save(captor.capture());

            User result = captor.getValue();

            assertEquals(user.getId(), result.getId());
            assertEquals(user.getUsername(), result.getUsername());
            assertEquals(user.getPassword(), result.getPassword());
            assertEquals(user.getEmail(), result.getEmail());
            assertEquals(user.getRoles(), result.getRoles());
            assertTrue(user.isAccountNonLocked());
            assertEquals(0, result.getFailedLoginAttempts()); // Expect 1 failed attempt
            assertNull(result.getLockedUntil());
        });
    }
}
