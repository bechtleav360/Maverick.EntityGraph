package org.av360.maverick.graph.api.security;


import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.eclipse.rdf4j.rio.RDFFormat;
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
@ActiveProfiles({"test", "api", "secure"})
public class TestUnauthenticated extends TestsBase {
    @Autowired
    private WebTestClient webClient;
    private boolean isPrepared = false;


    @Test
    public void listEntities() {
        this.prepare();

        super.printStart("list Entities without authentication");

        RdfConsumer rdfConsumer = new RdfConsumer(RDFFormat.JSONLD);
        webClient.get()
                .uri("/api/entities")
                .accept(MediaType.parseMediaType("application/ld+json"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .consumeWith(rdfConsumer);

        super.printModel(rdfConsumer.asModel(), RDFFormat.TURTLE);
    }

    @Test
    public void readEntity() {
        this.prepare();

        super.printStart("read entity without authentication");

        webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/entities/{id}")
                        .build("b7fljdhx")
                )
                .accept(MediaType.parseMediaType("application/ld+json"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    public void updateEntity() {
        this.prepare();

        super.printStart("update entity without authentication");

        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/entities/{id}/values/dc.description")
                        .build("b7fljdhx")

                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .body(BodyInserters.fromValue("this is the description"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    private void prepare() {
        if(!this.isPrepared) {
            Resource file = new ClassPathResource("requests/create-valid_multiple.ttl");
            webClient.post()
                    .uri("/api/entities")
                    .contentType(MediaType.parseMediaType("text/turtle"))
                    .header("X-API-KEY", "test")
                    .body(BodyInserters.fromResource(file))
                    .exchange()
                    .expectStatus().isAccepted();
        }


        this.isPrepared = true; 
    }
}
