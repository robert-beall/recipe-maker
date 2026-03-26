package com.recipe_maker.backend.users;

import java.time.Instant;
import java.util.Set;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.recipe_maker.backend.authentication.JwtService;
import com.recipe_maker.backend.authentication.LoginDTO;
import com.recipe_maker.backend.authentication.RefreshToken;
import com.recipe_maker.backend.authentication.RefreshTokenDTO;
import com.recipe_maker.backend.authentication.RefreshTokenRepository;
import com.recipe_maker.backend.authentication.TokenResponseDTO;
import com.recipe_maker.backend.roles.Role;
import com.recipe_maker.backend.roles.RoleName;
import com.recipe_maker.backend.roles.RoleRepository;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {
    /** The repository for managing user data. */
    private UserRepository userRepository;

    /** The repository for managing role data. */
    private RoleRepository roleRepository;

    /** The repository for managing refresh tokens. */
    private RefreshTokenRepository refreshTokenRepository;

    /** The ModelMapper for converting between DTOs and entities. */
    private ModelMapper modelMapper;

    /** The PasswordEncoder for encoding user passwords. */
    private PasswordEncoder passwordEncoder;

    /** The AuthenticationManager for handling authentication logic. */
    private AuthenticationManager authenticationManager;

    /** The JwtService for generating and validating JWT tokens. */
    private JwtService jwtService;

    /**
     * Constructor for UserService.
     * @param userRepository
     * @param modelMapper
     * @param passwordEncoder
     */
    public UserService(
        UserRepository userRepository, 
        RoleRepository roleRepository,
        ModelMapper modelMapper, 
        PasswordEncoder passwordEncoder,
        AuthenticationManager authenticationManager,
        JwtService jwtService,
        RefreshTokenRepository refreshTokenRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.modelMapper = modelMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
    } 

    /**
     * Checks if a user with the given username exists.
     * @param username
     * @return boolean indicating whether a user with the given username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Checks if a user with the given email exists.
     * @param email
     * @return boolean indicating whether a user with the given email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Registers a new user in the system.
     * @param userDTO
     */
    public void registerUser(UserDTO userDTO) {
        // Check for existing username and email
        if (existsByUsername(userDTO.getUsername())) {
            throw new DataIntegrityViolationException("Username already exists");
        }

        if (existsByEmail(userDTO.getEmail())) {
            throw new DataIntegrityViolationException("Email already exists");
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER)
        .orElseThrow(() -> new IllegalStateException("Default role not found"));

        // Map DTO to entity, encode password, and save user
        User user = modelMapper.map(userDTO, User.class);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setRoles(Set.of(userRole)); // Assign base USER role
        userRepository.save(user);
    }

    /**
     * Authenticates a user and generates JWT tokens upon successful login.
     * @param loginDTO
     * @return TokenResponseDTO containing the access and refresh tokens for the authenticated user
     */
    public TokenResponseDTO login(LoginDTO loginDTO) {
        // Attempt authentication using the provided credentials
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginDTO.getUsername(),
                loginDTO.getPassword()
            )
        );

        // If authentication is successful, retrieve the user and generate tokens
        User user = userRepository.findByUsername(loginDTO.getUsername())
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Parse the user roles as a Set<String>
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());
        
        // delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Generate a new refresh token and save it to the database
        String rawRefreshToken = jwtService.generateRefreshToken(loginDTO.getUsername(), roles);

        // Create a new RefreshToken entity and save it to the database
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(rawRefreshToken);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        refreshTokenRepository.save(refreshToken);

        // Generate and return the access and refresh tokens for the authenticated user
        return new TokenResponseDTO(
            jwtService.generateAccessToken(loginDTO.getUsername(), roles),
            rawRefreshToken
        );
    }

    /**
     * Refreshes the access token using a valid refresh token. If the refresh token 
     * is valid and not expired, a new access token and refresh token are generated 
     * and returned. If the refresh token is invalid or expired, an exception is thrown.
     * 
     * @param refreshTokenDTO containing the refresh token to be used for refreshing the access token
     * @return TokenResponseDTO containing the new access token and refresh token if the refresh token is valid, otherwise an exception is thrown
     */
    public TokenResponseDTO refresh(RefreshTokenDTO refreshTokenDTO) {
        // Find the refresh token in the database
        RefreshToken stored = refreshTokenRepository.findByToken(refreshTokenDTO.getRefreshToken())
            .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        // Check if the refresh token has expired
        if (stored.isExpired()) {
            // If the refresh token has expired, delete it from the database and throw an exception
            refreshTokenRepository.delete(stored);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        // If the refresh token is valid, generate a new access token and refresh token
        String username = stored.getUser().getUsername();

        // If authentication is successful, retrieve the user and generate tokens
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Parse the user roles as a Set<String>
        Set<String> roles = user.getRoles()
                .stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet());

        // rotate the refresh token — issue a new one each time
        refreshTokenRepository.delete(stored);
        String newRefreshToken = jwtService.generateRefreshToken(username, roles);

        // Create a new RefreshToken entity and save it to the database
        RefreshToken newStored = new RefreshToken();
        newStored.setToken(newRefreshToken);
        newStored.setUser(stored.getUser());
        newStored.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshTokenExpiration()));
        refreshTokenRepository.save(newStored);

        // Return the new access token and refresh token in a TokenResponseDTO
        return new TokenResponseDTO(
            jwtService.generateAccessToken(username, roles),
            newRefreshToken
        );
    }

    /**
     * Logout the User by deleting the associated RefreshToken.
     */
    public void logout() {
        // Get the username from the security context
        String username = SecurityContextHolder.getContext()
            .getAuthentication()
            .getName();

        // Find the user by the username. If no user is found, throw exception
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Delete the refresh token
        refreshTokenRepository.deleteByUser(user);
    }
}
