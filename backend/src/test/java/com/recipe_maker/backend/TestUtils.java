package com.recipe_maker.backend;

import java.util.List;

/**
 * Interface for providing test utilities for entity and DTO instances.
 * @param <Entity> the type of the entity
 * @param <DTO> the type of the DTO
 */
public interface TestUtils<Entity, DTO> {
    /**
     * Creates an instance of the entity for testing purposes.
     * @return Entity instance
     */
    public Entity createEntity();

    /**
     * Creates a list of entities for testing purposes.
     * @param size
     * @return List of Entity instances
     */
    public List<Entity> createEntityList(int size);

    /**
     * Creates an instance of the DTO for testing purposes.
     * @return DTO instance
     */
    public DTO createDTO();

    /**
     * Creates a list of DTOs for testing purposes.
     * @param size
     * @return List of DTO instances
     */
    public List<DTO> createDTOList(int size);
}
