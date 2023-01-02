package io.av360.maverick.graph.tests.api.v1;


public interface EntitiesTest {

    /**
     * <pre>POST /api/rs</pre>
     */
    void createEntity();

    void createEntityWithMissingType();

    /**
     * <pre>POST /api/rs</pre>
     */
    void createEntityWithInvalidSyntax();

    /**
     * <pre>POST /api/rs</pre>
     */
    void createEntityWithValidId();

    /**
     * <pre>POST /api/rs</pre>
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