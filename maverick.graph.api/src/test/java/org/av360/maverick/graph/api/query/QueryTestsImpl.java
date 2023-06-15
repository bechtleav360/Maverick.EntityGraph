package org.av360.maverick.graph.api.query;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.tests.api.v1.QueriesTest;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.CsvConsumer;
import org.av360.maverick.graph.tests.util.TestsBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test","api"})
@Slf4j
public class QueryTestsImpl extends TestsBase implements QueriesTest {

    @Autowired
    private WebTestClient webClient;

    @Override
    @Test
    public void runSparqlQuery() {

        webClient.post()
                .uri("/api/entities")
                .contentType(MediaType.parseMediaType("text/turtle"))
                .body(BodyInserters.fromResource(new ClassPathResource("requests/create-valid_multiple.ttl")))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody();

        // SELECT DISTINCT * WHERE { ?s ?p ?o }

        CsvConsumer csvConsumer = new CsvConsumer();
        webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/query/select")
                        .queryParam("repository", "entities")
                        .build()
                )
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .body(BodyInserters.fromValue("SELECT DISTINCT * WHERE { ?s ?p ?o }"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);


        Assertions.assertEquals(8, csvConsumer.getRows().size());
    }

    @Override
    public void runInvalidSparqlQuery() {

    }

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }
}
