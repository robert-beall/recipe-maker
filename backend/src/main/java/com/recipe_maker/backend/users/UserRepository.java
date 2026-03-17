package com.recipe_maker.backend.users;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository interface for managing user data.
 */
public interface UserRepository extends JpaRepository<User, Long> {
    /**
     * Finds a user by their username.
     * @param username
     * @return Optional of User
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user exists by their username.
     * @param username
     * @return boolean
     */
    boolean existsByUsername(String username);

    /**
     * Deletes a user by their username.
     * @param username
     */
    void deleteByUsername(String username);
}
