package com.bechtle.eagl.graph.api.v1;

import org.springframework.boot.test.context.SpringBootTest;

public interface EntitiesTest {

    /**
     * <pre>POST /api/rs</pre>
     *
     */
    void createEntity();

    /**
     *  <pre>POST /api/rs</pre>
     */
    void createEntityWithInvalidSyntax();

    /**
     *  <pre>POST /api/rs</pre>
     */
    void createEntityWithValidId();

    /**
     *  <pre>POST /api/rs</pre>
     */
    void createEntityWithInvalidId();

    /**
     * <pre>POST /api/rs</pre>
     */
    void createMultipleEntities();

    /**
     * <pre>POST /api/rs</pre>
     */
    void createMultipleEntitiesWithMixedIds();

    /**
     * POST /api/rs/{id}/{prefix.key}
     */
    void createValue();

    /**
     * POST /api/rs/{id}/{prefix.key}
     */
    void createEmbeddedEntity();

    /**
     * POST /api/rs/{id}/{prefix.key}
     */
    void createEdgeWithIdInPayload();

    /**
     * POST /api/rs/{id}/{prefix.key}/{id}
     */
    void createEdge();


    /**
     * POST /api/rs/{id}/{prefix.key}/{id}
     */
    void createEdgeWithInvalidDestinationId();


}