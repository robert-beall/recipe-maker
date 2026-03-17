package com.recipe_maker.backend.users;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Data Transfer Object for representing a user in the system.
 */
@AllArgsConstructor
@Data
@NoArgsConstructor
public class UserDTO {
    /** The unique identifier for the user. */
    private Long id; 

    /** The username for the user. */
    private String username;

    /** The password for the user. */
    private String password;

    /** The email for the user. */
    private String email;
}
