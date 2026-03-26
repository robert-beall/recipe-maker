package com.recipe_maker.backend.users;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.recipe_maker.backend.authentication.AuthenticationTestUtils;
import com.recipe_maker.backend.authentication.JwtService;
import com.recipe_maker.backend.authentication.LoginDTO;
import com.recipe_maker.backend.authentication.RefreshToken;
import com.recipe_maker.backend.authentication.RefreshTokenDTO;
import com.recipe_maker.backend.authentication.RefreshTokenRepository;
import com.recipe_maker.backend.authentication.TokenResponseDTO;
import com.recipe_maker.backend.roles.Role;
import com.recipe_maker.backend.roles.RoleRepository;
import com.recipe_maker.backend.roles.RoleTestUtils;

import net.datafaker.Faker;

/**
 * Test class for UserService, which manages user-related operations such as registration.
 */
public class UserServiceTest {
    /** The service for managing user-related operations. */
    @InjectMocks
    private UserService userService;

    /** The repository for managing user data. */
    @Mock 
    private UserRepository userRepository;

    /** The repository for managing role data. */
    @Mock
    private RoleRepository roleRepository;

    /** The repository for managing refresh tokens. */
    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    /** The ModelMapper for mapping between DTOs and entities. */
    @Mock
    private ModelMapper modelMapper;

    /** The PasswordEncoder for encoding user passwords. */
    @Mock
    private PasswordEncoder passwordEncoder;

    /** The AuthenticationManager for handling authentication logic. */
    @Mock 
    private AuthenticationManager authenticationManager;

    /** The JwtService for generating and validating JWT tokens. */
    @Mock
    private JwtService jwtService;

    /** The mocked SecurityContext instance. */
    @Mock
    private SecurityContext securityContext;

    /** The mocked Authentication instance. */
    @Mock
    private Authentication authentication;

    /** The utility class for creating test user data. */
    private UserTestUtils userTestUtils;

    /** The utility class for creating test role data. */
    private RoleTestUtils roleTestUtils;

    /** The utility class for creating test authentication data. */
    private AuthenticationTestUtils authenticationTestUtils;

    /** The auto-closeable resource for managing mock objects. */
    private AutoCloseable autoCloseable;

    /** The Faker instance for generating fake data. */
    private Faker faker;

    /** Constructor for initializing the test class. */
    public UserServiceTest() {
        userTestUtils = new UserTestUtils();
        authenticationTestUtils = new AuthenticationTestUtils();
        roleTestUtils = new RoleTestUtils();
        faker = new Faker();
    }

    /** Sets up the test environment before each test method is executed. */
    @BeforeEach
    void setUp() {
        autoCloseable = org.mockito.MockitoAnnotations.openMocks(this);
    }

    /** Tears down the test environment after each test method is executed. */
    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    /**
     * Tests the existsByUsername method of UserService to ensure that it
     * correctly identifies whether a user with a given username exists.
     */
    @Test
    void testExistsByUsername() {
        // Create a test user and set up the mock repository to return true for the user's username
        User user = userTestUtils.createEntity();
        String username = user.getUsername();
        when(userRepository.existsByUsername(username)).thenReturn(true);

        // Assert that the existsByUsername method returns true for the given username
        assertTrue(userService.existsByUsername(username));
    }

    /**
     * Tests the existsByUsername method of UserService to ensure that it correctly 
     * identifies whether a user with a given username does not exist.
     */
    @Test
    void testExistsByUsernameNotFound() {
        // Create a test user and set up the mock repository to return false for the user's username
        User user = userTestUtils.createEntity();
        String username = user.getUsername();
        when(userRepository.existsByUsername(username)).thenReturn(false);

        // Assert that the existsByUsername method returns false for the given username
        assertFalse(userService.existsByUsername(username));
    }

    /**
     * Tests the existsByEmail method of UserService to ensure that it correctly identifies 
     * whether a user with a given email exists.
     */
    @Test
    void testExistsByEmail() {
        // Create a test user and set up the mock repository to return true for the user's email
        User user = userTestUtils.createEntity();
        String email = user.getEmail();
        when(userRepository.existsByEmail(email)).thenReturn(true);

        // Assert that the existsByEmail method returns true for the given email
        assertTrue(userService.existsByEmail(email));
    }

    /**
     * Tests the existsByEmail method of UserService to ensure that it correctly identifies 
     * whether a user with a given email does not exist.
     */
    @Test
    void testExistsByEmailNotFound() {
        // Create a test user and set up the mock repository to return false for the user's email
        User user = userTestUtils.createEntity();
        String email = user.getEmail();
        when(userRepository.existsByEmail(email)).thenReturn(false);

        // Assert that the existsByEmail method returns false for the given email
        assertFalse(userService.existsByEmail(email));
    }

    /**
     * Tests the registerUser method of UserService to ensure that a user is registered correctly when valid data is provided.
      * It verifies that the userRepository's save method is called with the expected User entity.
      * It also checks that the appropriate methods are called on the modelMapper and passwordEncoder mocks.
      * @throws DataIntegrityViolationException if a user with the same username or email already exists
     */
    @Test
    void testRegisterUser() {
        // Create a UserDTO with test data
        UserDTO dto = userTestUtils.createDTO();

        // Create an expected User entity based on the UserDTO
        User expected = new User();
        expected.setUsername(dto.getUsername());
        expected.setPassword(dto.getPassword());
        expected.setEmail(dto.getEmail());

        Role role = roleTestUtils.createEntity();

        // Set up the mock repository and services to return the expected values
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenReturn(expected);
        when(modelMapper.map(dto, User.class)).thenReturn(expected);
        when(modelMapper.map(expected, UserDTO.class)).thenReturn(dto);
        when(passwordEncoder.encode(any())).thenReturn(dto.getPassword());

        // Capture the User entity passed to the userRepository's save method
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        
        // Call the registerUser method and verify that the userRepository's save method was called with the expected User entity
        userService.registerUser(dto);
        verify(userRepository, times(1)).save(userCaptor.capture());
        assertEquals(expected, userCaptor.getValue());
    }

    /**
     * Tests the registerUser method of UserService to ensure that a DataIntegrityViolationException is thrown when a user with the same username already exists.
      * It verifies that the userRepository's save method is not called when a duplicate username is detected.
      * It also checks that the appropriate methods are called on the modelMapper and passwordEncoder mocks.
      * @throws DataIntegrityViolationException if a user with the same username already exists
     */
    @Test
    void testRegisterUserDuplicateUsername() {
        // Create a UserDTO with test data
        UserDTO dto = userTestUtils.createDTO();
        
        // Create an expected User entity based on the UserDTO
        User expected = new User();
        expected.setUsername(dto.getUsername());
        expected.setPassword(dto.getPassword());
        expected.setEmail(dto.getEmail());

        // Set up the mock repository and services to return the expected values, including a duplicate username scenario
        when(userRepository.existsByUsername(any())).thenReturn(true);
        when(modelMapper.map(dto, User.class)).thenReturn(expected);
        when(modelMapper.map(expected, UserDTO.class)).thenReturn(dto);
        when(passwordEncoder.encode(any())).thenReturn(dto.getPassword());

        // Call the registerUser method and assert that a DataIntegrityViolationException is thrown due to the duplicate username, and verify that the userRepository's save method was not called
        assertThrows(DataIntegrityViolationException.class, () -> userService.registerUser(dto), "Username already exists");
        verify(userRepository, times(0)).save(any(User.class));
    }

    /**
     * Tests the registerUser method of UserService to ensure that a DataIntegrityViolationException is thrown when a user with the same email already exists.
      * It verifies that the userRepository's save method is not called when a duplicate email is detected.
      * It also checks that the appropriate methods are called on the modelMapper and passwordEncoder mocks.
      * @throws DataIntegrityViolationException if a user with the same email already exists
     */
    @Test
    void testRegisterUserDuplicateEmail() {
        // Create a UserDTO with test data
        UserDTO dto = userTestUtils.createDTO();
        
        // Create an expected User entity based on the UserDTO
        User expected = new User();
        expected.setUsername(dto.getUsername());
        expected.setPassword(dto.getPassword());
        expected.setEmail(dto.getEmail());
        
        // Set up the mock repository and services to return the expected values, including a duplicate email scenario
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(true);
        when(modelMapper.map(dto, User.class)).thenReturn(expected);
        when(modelMapper.map(expected, UserDTO.class)).thenReturn(dto);
        when(passwordEncoder.encode(any())).thenReturn(dto.getPassword());

        // Call the registerUser method and assert that a DataIntegrityViolationException is thrown due to the duplicate email, and verify that the userRepository's save method was not called
        assertThrows(DataIntegrityViolationException.class, () -> userService.registerUser(dto));
        verify(userRepository, times(0)).save(any(User.class));
    }

    /**
     * Tests the registerUser method of UserService to ensure that an IllegalStateException
     * is thrown when the base role does not exist in the database.
     */
    @Test
    void testRegisterUserRoleNotFound() {
        // Create a UserDTO with test data
        UserDTO dto = userTestUtils.createDTO();
        
        // Create an expected User entity based on the UserDTO
        User expected = new User();
        expected.setUsername(dto.getUsername());
        expected.setPassword(dto.getPassword());
        expected.setEmail(dto.getEmail());
        
        // Set up the mock repository and services to return the expected values, including a duplicate email scenario
        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(roleRepository.findByName(any())).thenReturn(Optional.empty());

        // Call the registerUser method and assert that a DataIntegrityViolationException is thrown due to the duplicate email, and verify that the userRepository's save method was not called
        assertThrows(IllegalStateException.class, () -> userService.registerUser(dto));
        verify(userRepository, times(0)).save(any(User.class));
    }

    /**
     * Tests the login method of UserService to ensure that a user can log in successfully with valid credentials.
      * It verifies that the authenticationManager's authenticate method is called with the expected authentication token.
      * It also checks that the jwtService's generateAccessToken and generateRefreshToken methods are called to generate the appropriate tokens.
     */
    @Test
    void testLogin() {
        // Create a LoginDTO with test credentials
        LoginDTO loginDTO = authenticationTestUtils.createLoginDTO();

        User user = new User();
        user.setUsername(loginDTO.getUsername());
        user.setPassword(loginDTO.getPassword());

        RefreshToken refreshTokenEntity = authenticationTestUtils.createRefreshToken();

        // Generate a random token using Faker for testing purposes
        String token = faker.internet().uuid();

        // Set up the mock authenticationManager to return null (indicating successful authentication)
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(any(), any())).thenReturn(token);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn(token);
        when(jwtService.getRefreshTokenExpiration()).thenReturn(faker.number().randomNumber());
        doNothing().when(refreshTokenRepository).deleteByUser(any());
        when(refreshTokenRepository.save(any())).thenReturn(refreshTokenEntity);

        // Call the login method and capture the response
        TokenResponseDTO response = userService.login(loginDTO);

        // Verify that the authenticationManager's authenticate method was called with the expected authentication token
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateAccessToken(any(), any());
        verify(jwtService, times(1)).generateRefreshToken(any(), any());
        verify(refreshTokenRepository, times(1)).deleteByUser(any());
        verify(refreshTokenRepository, times(1)).save(any());
        
        // Assert that the access and refresh tokens in the response match the expected token
        assertEquals(token, response.getAccessToken());
        assertEquals(token, response.getRefreshToken());
    }

    /**
     * Tests the login method of UserService when the username cannot be found.
     */
    @Test
    void testLoginUserNotFound() {
        // Create a LoginDTO with test credentials
        LoginDTO loginDTO = authenticationTestUtils.createLoginDTO();

        User user = new User();
        user.setUsername(loginDTO.getUsername());
        user.setPassword(loginDTO.getPassword());

        // Set up the mock authenticationManager to return null (indicating successful authentication)
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        // Call the login method and capture the response
        assertThrows(UsernameNotFoundException.class, () -> userService.login(loginDTO), "User not found");

        // Verify that the authenticationManager's authenticate method was called with the expected authentication token
        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
        verify(refreshTokenRepository, never()).deleteByUser(any());
        verify(refreshTokenRepository, never()).save(any());
    }

    /**
     * Tests the refresh method of UserService to ensure that a user can refresh their tokens successfully.
     * It verifies that the jwtService's isTokenValid and extractUsername methods are called with the expected token.
     * It also checks that the jwtService's generateAccessToken and generateRefreshToken methods are called to generate the appropriate tokens.
     * @throws IllegalArgumentException if the refresh token is invalid
     */
    @Test
    void testRefresh() {
        // Create a LoginDTO and RefreshTokenDTO with test data
        LoginDTO loginDTO = authenticationTestUtils.createLoginDTO();
        User user = userTestUtils.createEntity();
        user.setUsername(loginDTO.getUsername());
        user.setPassword(loginDTO.getPassword());

        // Create new RefreshToken for testing
        RefreshToken refreshToken = authenticationTestUtils.createRefreshToken();

        // Create DTO from refreshToken entity
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO();
        refreshTokenDTO.setRefreshToken(refreshToken.getToken());

        // Generate a random token using Faker for testing purposes
        String token = faker.internet().uuid();

        // Set up the mock jwtService to validate the token and extract the username, as well as generate new tokens
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        when(jwtService.isTokenValid(any())).thenReturn(true);
        when(jwtService.extractUsername(any())).thenReturn(loginDTO.getUsername());
        when(jwtService.generateAccessToken(any(), any())).thenReturn(token);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn(token);

        // Call the refresh method and capture the response
        TokenResponseDTO response = userService.refresh(refreshTokenDTO);
        // Verify that the jwtService's isTokenValid and extractUsername methods were called with the expected token
        verify(refreshTokenRepository, times(1)).findByToken(any());
        verify(refreshTokenRepository, times(1)).delete(any());
        verify(jwtService, times(1)).generateAccessToken(any(), any());
        verify(jwtService, times(1)).generateRefreshToken(any(), any());

        // Assert that the access and refresh tokens in the response match the expected token
        assertEquals(token, response.getAccessToken());
        assertEquals(token, response.getRefreshToken());
    }

    /**
     * Tests the refresh method of UserService to ensure that an invalid refresh token results in an IllegalArgumentException.
     * @throws IllegalArgumentException if the refresh token is invalid
     */
    @Test
    void testRefreshTokenNotFound() {
        // Create a RefreshTokenDTO with test data
        RefreshTokenDTO refreshTokenDTO = authenticationTestUtils.createRefreshTokenDTO();

        // Set up the mock jwtService to return false for token validation
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.empty());

        // Call the refresh method and assert that an IllegalArgumentException is thrown due to the invalid token
        assertThrows(IllegalArgumentException.class, () -> userService.refresh(refreshTokenDTO));

        // Verify that the jwtService's isTokenValid method was called with the expected token, and that the extractUsername, generateAccessToken, and generateRefreshToken methods were not called
        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    /**
     * Tests the refresh method of UserService to ensure that an expired refresh token results in an IllegalArgumentException.
     * @throws IllegalArgumentException if the refresh token is invalid
     */
    @Test
    void testRefreshTokenExpired() {
        // Create a RefreshToken with test data and a matching RefreshTokenDTO
        RefreshToken refreshToken = authenticationTestUtils.createRefreshToken();

        // Set the token to be expired
        refreshToken.setExpiresAt(Instant.now());

        // Create new RefreshTokenDTO from RefreshToken test instance
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO(refreshToken.getToken());

        // Set up the mock jwtService to return false for token validation
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));

        // Call the refresh method and assert that an IllegalArgumentException is thrown due to the invalid token
        assertThrows(IllegalArgumentException.class, () -> userService.refresh(refreshTokenDTO));

        // Verify that the jwtService's isTokenValid method was called with the expected token, and that the extractUsername, generateAccessToken, and generateRefreshToken methods were not called
        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    /**
     * Tests the refresh method of UserService to ensure that an invalid username results in an UsernameNotFound exception.
     * @throws UsernameNotFoundException if the refresh token is invalid
     */
    @Test
    void testRefreshUserNotFound() {
        // Create a RefreshToken with test data and a matching RefreshTokenDTO
        RefreshToken refreshToken = authenticationTestUtils.createRefreshToken();

        // Create new RefreshTokenDTO from RefreshToken test instance
        RefreshTokenDTO refreshTokenDTO = new RefreshTokenDTO(refreshToken.getToken());

        // Set up the mock jwtService to return false for token validation
        when(refreshTokenRepository.findByToken(any())).thenReturn(Optional.of(refreshToken));
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());

        // Call the refresh method and assert that an IllegalArgumentException is thrown due to the invalid token
        assertThrows(UsernameNotFoundException.class, () -> userService.refresh(refreshTokenDTO));

        // Verify that the jwtService's isTokenValid method was called with the expected token, and that the extractUsername, generateAccessToken, and generateRefreshToken methods were not called
        verify(refreshTokenRepository, never()).save(any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    /**
     * Test the logout() method when logout is successful.
     */
    @Test
    void testLogout() {
        // Mocked user data
        User user = userTestUtils.createEntity();

        // Mock Spring security responses
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getUsername());
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(user));
        
        // Mock static SecurityContextHolder method getContext()
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(() -> SecurityContextHolder.getContext()).thenReturn(securityContext);

            assertDoesNotThrow(() -> userService.logout());
            verify(refreshTokenRepository, times(1)).deleteByUser(user);
        }
    }

     /**
     * Test the logout() method when logout fails because the user
     * is not found.
     */
    @Test
    void testLogoutUserNotFound() {
        // Mock Spring security responses
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(faker.credentials().username());
        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        
        // Mock static SecurityContextHolder method getContext()
        try (MockedStatic<SecurityContextHolder> mockedStatic = Mockito.mockStatic(SecurityContextHolder.class)) {
            mockedStatic.when(() -> SecurityContextHolder.getContext()).thenReturn(securityContext);

            assertThrows(UsernameNotFoundException.class, () -> userService.logout(), "User not found");
            verify(refreshTokenRepository, never()).deleteByUser(any());
        }
    }
}
