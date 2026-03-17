package com.recipe_maker.backend.users;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;

public class UserServiceTest {
    @InjectMocks
    private UserService userService;

    @Mock 
    private UserRepository userRepository;

    @Mock
    private ModelMapper modelMapper;

    private UserTestUtils userTestUtils;

    private AutoCloseable autoCloseable;

    public UserServiceTest() {
        userTestUtils = new UserTestUtils();
    }

    @BeforeEach
    void setUp() {
        autoCloseable = org.mockito.MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testRegisterUser() {
        UserDTO expected = userTestUtils.createDTO();
        User expectedEntity = new User(
            expected.getId(), 
            expected.getUsername(), 
            expected.getPassword(), 
            expected.getEmail());

        when(userRepository.existsByUsername(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(expectedEntity);
        when(modelMapper.map(expected, User.class)).thenReturn(expectedEntity);
        when(modelMapper.map(expectedEntity, UserDTO.class)).thenReturn(expected);
    
        UserDTO actual = userService.registerUser(expected);
        verify(userRepository, times(1)).save(any(User.class));
        assertEquals(expected, actual);
    }
}
