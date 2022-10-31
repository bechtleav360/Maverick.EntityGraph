package io.av360.maverick.graph.api.v1.ttl;

import io.av360.maverick.graph.boot.TestConfigurations;
import io.av360.maverick.graph.store.rdf.helpers.RdfUtils;
import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.model.vocabulary.Transactions;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.tests.api.v1.EntitiesTest;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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

import java.util.Collection;
import java.util.stream.StreamSupport;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
@Slf4j
public class TurtleTests extends TestsBase implements EntitiesTest {


    public static ValueFactory vf = SimpleValueFactory.getInstance();

    @Autowired
    private WebTestClient webClient;



    @AfterEach
    public void resetRepository() {
        super.resetRepository(RepositoryType.ENTITIES.name());
    }

    @Override
    @Test
    public void createEntity() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");


        Collection<Statement> statements = rdfConsumer.getStatements();
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));

        // check if correct application events have been recorded
    }

    @Override
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

    @Override
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

    @Override
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
    @Override
    /**
     * The parser fails here, bug was reported: https://github.com/eclipse/rdf4j/issues/3658
     */
    public void createEntityWithInvalidId() {
        Resource file = new ClassPathResource("requests/create-validWithInvalidId.ttl");

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
        RdfConsumer rdfConsumer = super.upload("requests/create-valid_multiple.ttl");

        Collection<Statement> statements = rdfConsumer.getStatements();

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

    @Override
    public void createMultipleEntitiesWithMixedIds() {
        Assertions.assertTrue(true);
    }









    @Override
    @Test
    public void createEmbeddedEntity() {
        RdfConsumer rdfConsumer = super.upload("requests/create-valid.ttl");
        Assertions.assertTrue(rdfConsumer.hasStatement(null, Transactions.STATUS, Transactions.SUCCESS));

        Statement video = rdfConsumer.findStatement(null, RDF.TYPE, vf.createIRI("http://schema.org/", "video"));


        Resource embedded = new ClassPathResource("requests/create-valid_embedded.ttl");

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/sdo.hasDefinedTerm")
                        .build(
                                vf.createIRI(video.getSubject().stringValue()).getLocalName()
                        )

                )
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(embedded))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .consumeWith(rdfConsumer);

        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), video.getPredicate(), video.getObject()));
        Assertions.assertTrue(rdfConsumer.hasStatement(video.getSubject(), vf.createIRI("http://schema.org/hasDefinedTerm"), null));
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
