package org.av360.maverick.graph.api.entities.formats;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.model.vocabulary.meg.Transactions;
import org.av360.maverick.graph.tests.clients.EntitiesTestClient;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.*;
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

import java.util.Collection;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class CreateEntitiesInTurtleTests extends ApiTestsBase {
    @Autowired
    private WebTestClient webClient;

    EntitiesTestClient entitiesTestClient;

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @BeforeEach
    public void prepare() {
        this.entitiesTestClient = new EntitiesTestClient(webClient);
    }

    @Test
    public void createEntity() {

        super.printStart("Creating entity in turtle syntax");
        RdfConsumer rdfConsumer = this.entitiesTestClient.upload("requests/create-valid.ttl");

        super.printModel(rdfConsumer.asModel(), RDFFormat.TURTLE);
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));
    }

    @Test
    public void createEntityWithMissingType() {
        Resource file = new ClassPathResource("requests/create-invalid-missingType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void createEntityWithInvalidSyntax() {
        Resource file = new ClassPathResource("requests/create-invalid-syntax.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()

                .expectStatus().isBadRequest();
    }

    @Test
    public void createEntityWithValidId() {
        RdfConsumer rdfConsumer = super.upload("requests/create-validWithId.ttl");

        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));

    }

    @Test
    public void createEntityWithValidIdAndBase() {
        RdfConsumer rdfConsumer = super.upload("requests/create-validWithIdAndBase.ttl");

        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));
    }

    @Test
    public void createEntityWithInvalidId() {
        Resource file = new ClassPathResource("requests/create-validWithInvalidId.ttl");

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    public void createMultipleEntities() {
        super.printStart("Creating multiple entities using request: create-valid_multiple.ttl ");
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_multiple.ttl");


        rdfConsumer.print();
        Assertions.assertFalse(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.RUNNING));
        Assertions.assertTrue(rdfConsumer.hasStatement(null, SDO.IDENTIFIER, SimpleValueFactory.getInstance().createLiteral("_a")));
        Assertions.assertTrue(rdfConsumer.hasStatement(null, RDF.TYPE, SDO.VIDEO_OBJECT));
    }

    @Test
    public void createMultipleEntitiesWithNoType() {
        Resource file = new ClassPathResource("requests/create-invalid_multipleOneNoType.ttl");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @Disabled
    public void createMultipleEntitiesWithMixedIds() {

    }
}
