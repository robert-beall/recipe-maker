package com.recipe_maker.backend.users;

import java.util.List;

import com.recipe_maker.backend.TestUtils;

import net.datafaker.Faker;

public class UserTestUtils implements TestUtils<User, UserDTO> {
    /** The Faker instance for generating test data. */
    private Faker faker;

    public UserTestUtils() {
        faker = new Faker();
    }

    /**
     * Creates an instance of the User entity for testing purposes.
     * @return User instance
     */
    public User createEntity() {
        Long id = faker.number().randomNumber();
        String username = faker.credentials().username();
        String email = faker.internet().emailAddress();
        String password = faker.credentials().password();

        return new User(id, username, password, email);
    }

    /**
     * Creates a list of User entities for testing purposes.
     * @param size the number of User entities to create
     * @return List of User instances
     */
    public List<User> createEntityList(int size) {
        return faker.collection(() -> createEntity()).len(size).generate();
    }

    /**
     * Creates an instance of the UserDTO for testing purposes.
     * @return UserDTO instance
     */
    public UserDTO createDTO() {
        Long id = faker.number().randomNumber();
        String username = faker.credentials().username();
        String email = faker.internet().emailAddress();
        String password = faker.credentials().password();

        return new UserDTO(id, username, password, email);
    }

    /**
     * Creates a list of UserDTO instances for testing purposes.
     * @param size the number of UserDTO instances to create
     * @return List of UserDTO instances
     */
    public List<UserDTO> createDTOList(int size) {
        return faker.collection(() -> createDTO()).len(size).generate();
    }
    
}
