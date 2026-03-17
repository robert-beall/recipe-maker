package com.recipe_maker.backend.users;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("/users")
public class UserController {
    private final UserService userService;

    /**
     * Constructor for UserController.
     * @param userService
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Endpoint for registering a new user.
     * @param userDTO
     * @return UserDTO of user to register
     */
    @PostMapping("/register")
    public UserDTO registerUser(UserDTO userDTO) {
        return userService.registerUser(userDTO);
    }
}
