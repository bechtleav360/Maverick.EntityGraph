package io.av360.maverick.graph.tests.api.v3;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * scope:
 * - Patching of entities
 * -
 *
 * @version 3
 */
@SpringBootTest
interface EntitiesTest {

    /**
     *
     */
    void createEntity();

    /**
     *
     */
    void createEntityWithInvalidSyntax();

    /**
     *
     */
    void createEntityWithValidId();

    /**
     *
     */
    void createEntityWithInvalidId();

    /**
     *
     */
    void createMultipleEntities();

    /**
     *
     */
    void createMultipleEntitiesWithMixedIds();

    /**
     *
     */
    void createEntityWithAnnotations();

    /**
     *
     */
    void readEntityById();

    /**
     *
     */
    void readEntityByInvalidId();

    /**
     *
     */
    void readEntityByExample();

    /**
     *
     */
    void readEntityWithAnnotations();

    /**
     *
     */
    void createEmbeddedEntity();


}