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
public class JsonLdEntitiesTest implements EntitiesTest {


    @Autowired
    private WebTestClient webClient;


    @Override
    @Test
    public void createEntity() {
        Resource file = new ClassPathResource("data/v1/requests/create-valid.jsonld");
        webClient.post()
                .uri("/api/rs")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();


        // check if correct application events have been recorded

    }

    @Override
    public void createEntityWithInvalidSyntax() {

    }

    @Override
    public void createEntityWithValidId() {

    }

    @Override
    public void createEntityWithInvalidId() {

    }

    @Override
    public void createMultipleEntities() {

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
