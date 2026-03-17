package com.recipe_maker.backend.users;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service class for managing users.
 */
@Service
@Transactional
public class UserService {
    /** The repository for managing user data. */
    private UserRepository userRepository;

    /** The ModelMapper for converting between DTOs and entities. */
    private ModelMapper modelMapper;

    /**
     * Constructor for UserService.
     * @param userRepository
     * @param modelMapper
     */
    public UserService(UserRepository userRepository, ModelMapper modelMapper) {
        this.userRepository = userRepository;
        this.modelMapper = modelMapper;
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
     * Registers a new user in the system.
     * @param userDTO
     * @return UserDTO of the registered user
     */
    public UserDTO registerUser(UserDTO userDTO) {
        if (existsByUsername(userDTO.getUsername())) {
            throw new DataIntegrityViolationException("Username already exists");
        }

        User user = modelMapper.map(userDTO, User.class);
        return modelMapper.map(userRepository.save(user), UserDTO.class);
    }
}
