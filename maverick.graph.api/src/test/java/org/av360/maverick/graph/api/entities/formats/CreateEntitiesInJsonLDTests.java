package org.av360.maverick.graph.api.entities.formats;

import org.av360.maverick.graph.model.vocabulary.Transactions;
import org.av360.maverick.graph.tests.clients.EntitiesTestClient;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.web.reactive.function.BodyInserters;

import java.util.Collection;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class CreateEntitiesInJsonLDTests extends ApiTestsBase {
    private EntitiesTestClient client;


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @BeforeEach
    public void setup() {
        client = new EntitiesTestClient(super.webClient);
    }

    @Test
    public void createEntity() {
        RdfConsumer rdfConsumer = client.createEntity(new ClassPathResource("requests/create-valid.jsonld"));

        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));

    }

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

    @Test
    @Disabled
    public void createMultipleEntitiesWithMixedIds() {

    }
}
