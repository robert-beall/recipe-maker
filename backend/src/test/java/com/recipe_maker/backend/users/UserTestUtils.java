package com.recipe_maker.backend.users;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.recipe_maker.backend.TestUtils;
import com.recipe_maker.backend.roles.Role;
import com.recipe_maker.backend.roles.RoleDTO;
import com.recipe_maker.backend.roles.RoleTestUtils;

import net.datafaker.Faker;

public class UserTestUtils implements TestUtils<User, UserDTO> {
    /** The Faker instance for generating test data. */
    private Faker faker;

    private RoleTestUtils roleTestUtils;

    /**
     * Constructor for test utils class.
     */
    public UserTestUtils() {
        faker = new Faker();
        roleTestUtils = new RoleTestUtils();
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
        Set<Role> roles = new HashSet<>(roleTestUtils.createEntityList(2));


        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setEmail(email);
        user.setRoles(roles);

        return user;
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
        String username = faker.credentials().username();
        String email = faker.internet().emailAddress();
        String password = faker.credentials().password(8, 16);

        return new UserDTO(username, password, email);
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
