package com.recipe_maker.backend.authentication;

import java.util.Date;
import java.util.Set;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * Service for handling JWT operations.
 */
@Service
public class JwtService {

    /** The JWT secret key. */
    @Value("${jwt.secret}")
    private String secret;

    /** The expiration time for access tokens. */
    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;

    /** The expiration time for refresh tokens. */
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;

    /**
     * Gets the signing key for JWT operations.
     * @return SecretKey used for signing JWTs.
     */
    protected SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    /**
     * Generates an access token for the given username.
     * @param username The username for which to generate the token.
     * @return Generated JWT access token as a String.
     */
    public String generateAccessToken(String username, Set<String> roles) {
        return generateToken(username, roles, accessTokenExpiration);
    }

    /**
     * Generates a refresh token for the given username.
     * @param username The username for which to generate the token.
     * @return Generated JWT refresh token as a String.
     */
    public String generateRefreshToken(String username, Set<String> roles) {
        return generateToken(username, roles, refreshTokenExpiration);
    }

    /**
     * Generates a JWT token with the specified username and expiration time.
     * @param username The username for which to generate the token.
     * @param expiration The expiration time for the token.
     * @return Generated JWT token as a String.
     */
    protected String generateToken(String username, Set<String> roles, long expiration) {
        // Set the issued at and expiration times for the token
        return Jwts.builder()
            .subject(username)
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey())
            .compact();
    }

    /**
     * Extracts the username from the given JWT token.
     * @param token The JWT token from which to extract the username.
     * @return The username extracted from the token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates the given JWT token.
     * @param token The JWT token to validate.
     * @return true if the token is valid, false otherwise.
     */
    public boolean isTokenValid(String token) {
        try {
            // Attempt to parse the token to verify its validity
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Extracts a specific claim from the given JWT token using 
     * the provided claims resolver function.
     * @param <T> The type of the claim to extract.
     * @param token The JWT token from which to extract the claim.
     * @param claimsResolver The function to resolve the claim from the claims.
     * @return The extracted claim.
     */
    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
        return claimsResolver.apply(claims);
    }

    /**
     * Gets the expiration time for access tokens.
     * @return The expiration time for access tokens in milliseconds.
     */
    public long getAccessTokenExpiration() {
        return accessTokenExpiration;
    }

    /**
     * Gets the expiration time for refresh tokens.
     * @return The expiration time for refresh tokens in milliseconds.
     */
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
