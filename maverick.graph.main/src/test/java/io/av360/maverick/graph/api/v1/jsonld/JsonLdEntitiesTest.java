package io.av360.maverick.graph.api.v1.jsonld;

import io.av360.maverick.graph.boot.TestConfigurations;
import io.av360.maverick.graph.tests.api.v1.EntitiesTest;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
public class JsonLdEntitiesTest extends TestsBase implements EntitiesTest {


    @Autowired
    private WebTestClient webClient;


    @Override
    @Test
    public void createEntity() {
        Resource file = new ClassPathResource("requests/create-valid.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();


        // check if correct application events have been recorded

    }

    @Override
    @Test
    public void createEntityWithMissingType() {
        Resource file = new ClassPathResource("requests/create-invalid-missingType.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithInvalidSyntax() {
        Resource file = new ClassPathResource("requests/create-invalid-syntax.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithValidId() {
        Resource file = new ClassPathResource("requests/create-validWithId.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();

    }

    @Test
    public void createEntityWithValidIdAndBase() {
        Resource file = new ClassPathResource("requests/create-validWithIdAndBase.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();

    }

    @Test
    @Override
    /**
     * The parser fails here, bug was reported: https://github.com/eclipse/rdf4j/issues/3658
     */
    public void createEntityWithInvalidId() {
        Resource file = new ClassPathResource("requests/create-validWithInvalidId.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createMultipleEntities() {
        Resource file = new ClassPathResource("requests/create-valid_multiple.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();
    }


    @Test
    public void createMultipleEntitiesWithNoType() {
        Resource file = new ClassPathResource("requests/create-invalid_multipleOneNoType.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    public void createMultipleEntitiesWithMixedIds() {

    }

    @Override
    public void createValue() {

    }

    @Override
    public void createEmbeddedEntity() {

    }

    @Override
    public void createEdgeWithIdInPayload() {

    }

    @Override
    public void createEdge() {

    }

    @Override
    public void createEdgeWithInvalidDestinationId() {

    }
}
