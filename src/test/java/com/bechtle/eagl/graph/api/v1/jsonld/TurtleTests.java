package com.bechtle.eagl.graph.api.v1.jsonld;

import com.bechtle.eagl.graph.api.v1.EntitiesTest;
import config.TestConfigurations;
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
public class TurtleTests implements EntitiesTest {


    @Autowired
    private WebTestClient webClient;


    @Override
    @Test
    public void createEntity() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();


        // check if correct application events have been recorded

    }

    @Override
    @Test
    public void createEntityWithMissingType() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid-missingType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithInvalidSyntax() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid-syntax.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createEntityWithValidId() {
        Resource file = new ClassPathResource("data/v1/requests/create-validWithId.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();

    }

    @Test
    public void createEntityWithValidIdAndBase() {
        Resource file = new ClassPathResource("data/v1/requests/create-validWithIdAndBase.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
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
        Resource file = new ClassPathResource("data/v1/requests/create-validWithInvalidId.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Override
    @Test
    public void createMultipleEntities() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid_multiple.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();
    }


    @Test
    public void createMultipleEntitiesWithNoType() {
        Resource file = new ClassPathResource("data/v1/requests/create-invalid_multipleOneNoType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
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
