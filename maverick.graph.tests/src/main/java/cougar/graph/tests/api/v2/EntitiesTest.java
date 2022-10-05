package cougar.graph.tests.api.v2;

import org.springframework.boot.test.context.SpringBootTest;

/**
 * Scope:
 * - Paging support
 * - HATEOAS navigation
 * - Read Entities
 * - Read values
 * - Read edges
 * - Delete entities
 * - Delete values
 * - Delete Edges
 * - Update Values
 *
 *
 * @version 2
 */
@SpringBootTest
interface EntitiesTest {

    /**
     * <pre>POST /api/rs</pre>
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



}