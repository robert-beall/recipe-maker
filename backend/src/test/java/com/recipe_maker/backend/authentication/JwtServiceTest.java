package com.recipe_maker.backend.authentication;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.recipe_maker.backend.roles.RoleTestUtils;

import net.datafaker.Faker;

/**
 * Test class for JwtService class.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = JwtService.class)
@TestPropertySource(locations = "classpath:application.properties")
public class JwtServiceTest {
    /** The JwtService to test. */
    @Autowired
    private JwtService jwtService;

    /** Faker instance from generating test data. */
    private Faker faker = new Faker(); 

    /** Utils class to generate role test data. */
    private RoleTestUtils roleTestUtils = new RoleTestUtils();

    /** Access token expiration test value. */
    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    /** Refresh token expiration test value. */
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    /**
     * Test getSigninKey() method when successful.
     */
    @Test
    void testGetSigninKey() {
        // generating a key should return a non-null value and not through exceptions.
        assertDoesNotThrow(() -> {
            SecretKey secretKey = jwtService.getSigningKey();
            assertNotNull(secretKey);
        });
    }

    /**
     * Test generateAccessToken() method when successful.
     */
    @Test
    void testGenerateAccessToken() {
        // Generate username and expiration
        String username = faker.credentials().username();

        // Generate role data
        Set<String> roles = roleTestUtils.createEntityList(2)
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        // Assert that token generation does not throw exception
        assertDoesNotThrow(() -> {
            // Generate and store token
            String token = jwtService.generateAccessToken(username, roles);

            // Assert that the token is not null
            assertNotNull(token);
        });
    }

    /**
     * Test generateRefreshToken() method when successful.
     */
    @Test
    void testGenerateRefreshToken() {
        // Generate username and expiration
        String username = faker.credentials().username();

        // Generate role data
        Set<String> roles = roleTestUtils.createEntityList(2)
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        // Assert that token generation does not throw exception
        assertDoesNotThrow(() -> {
            // Generate and store token
            String token = jwtService.generateRefreshToken(username, roles);

            // Assert that the token is not null
            assertNotNull(token);
        });
    }

    /**
     * Test generateToken() method when successful.
     */
    @Test
    void testGenerateToken() {
        // Generate username and expiration
        String username = faker.credentials().username();
        Long expiration = faker.number().randomNumber();

        // Generate role data
        Set<String> roles = roleTestUtils.createEntityList(2)
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        // Assert that token generation does not throw exception
        assertDoesNotThrow(() -> {
            // Generate and store token
            String token = jwtService.generateToken(username, roles, expiration);

            // Assert that the token is not null
            assertNotNull(token);
        });
    }

    /**
     * Test extractUsername() method.
     */
    @Test
    void testExtractUsername() {
        // Generate test username
        String expected = faker.credentials().username();

        // Generate role data
        Set<String> roles = roleTestUtils.createEntityList(2)
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        // Create a token from username
        String token = jwtService.generateAccessToken(expected, roles);

        // Store method results
        String actual = jwtService.extractUsername(token);

        // Assert results match expected value
        assertEquals(expected, actual);
    }

    /**
     * Test isTokenValid() method when successful.
     */
    @Test
    void testIsTokenValid() {
        // Generate username and expiration
        String username = faker.credentials().username();

        // Generate role data
        Set<String> roles = roleTestUtils.createEntityList(2)
                .stream()
                .map(role -> role.getName().toString())
                .collect(Collectors.toSet());

        String token = jwtService.generateToken(username, roles, refreshTokenExpiration);

        assertTrue(jwtService.isTokenValid(token));
    }
    
    /**
     * Test isTokenValid() method when validation fails.
     */
    @Test
    void testIsTokenValueInvalid() {
        assertFalse(jwtService.isTokenValid(faker.internet().uuid()));
    }

    /**
     * Test getAccessTokenExpiration() method with test values.
     */
    @Test
    void testGetAccessTokenExpiration() {
        // Call service method for value
        long results = jwtService.getAccessTokenExpiration();

        // Assert the result matches test properties
        assertEquals(accessTokenExpiration, results);
    }

    /**
     * Test getRefreshTokenExpiration() method with test values.
     */
    @Test
    void testGetRefreshTokenExpiration() {
        // Call service method for value
        long results = jwtService.getRefreshTokenExpiration();

        // Assert the result matches test properties
        assertEquals(refreshTokenExpiration, results);
    }
}
