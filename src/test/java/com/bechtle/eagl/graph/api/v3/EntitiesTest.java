package com.bechtle.eagl.graph.api.v3;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * scope:
 *  - Patching of entities
 *  -
 *
 * @version 3
 */
@SpringBootTest
interface EntitiesTest {

    /**
     *
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