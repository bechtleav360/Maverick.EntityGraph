package org.av360.maverick.graph.api.entities;

import io.av360.maverick.graph.tests.config.TestSecurityConfig;
import io.av360.maverick.graph.tests.util.ApiTestsBase;
import io.av360.maverick.graph.tests.util.RdfConsumer;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class ListEntities extends ApiTestsBase {

    @Autowired
    private WebTestClient webClient;

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void listEntities() {
        Resource file = new ClassPathResource("requests/create-valid_many.jsonld");
        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("application/ld+json"))
                .body(BodyInserters.fromResource(file))
                .exchange()
                .expectStatus().isAccepted();

        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.JSONLD);
        webClient.get()
                .uri("/api/entities")
                .accept(MediaType.parseMediaType("application/ld+json"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);
        System.out.println(rdfConsumer.dump(RDFFormat.TURTLE));

        Assertions.assertEquals(9, (long) rdfConsumer.asModel().filter(null, RDF.TYPE, null).size());

        // check if correct application events have been recorded

    }
}
