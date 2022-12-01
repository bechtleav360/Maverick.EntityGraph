package io.av360.maverick.graph.main.api.v1;

import io.av360.maverick.graph.main.boot.TestConfigurations;
import io.av360.maverick.graph.store.RepositoryType;
import io.av360.maverick.graph.tests.api.v1.QueriesTest;
import io.av360.maverick.graph.tests.util.CsvConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import lombok.extern.slf4j.Slf4j;
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
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
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
                .uri("/api/query/select")
                .contentType(MediaType.parseMediaType("text/plain"))
                .accept(MediaType.parseMediaType("text/csv"))
                .body(BodyInserters.fromValue("SELECT DISTINCT * WHERE { ?s ?p ?o }"))
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .consumeWith(csvConsumer);



        Assertions.assertEquals(6, csvConsumer.getRows().size());
    }

    @Override
    public void runInvalidSparqlQuery() {

    }

    @AfterEach
    public void resetRepository() {
        super.resetRepository(RepositoryType.ENTITIES.name());
    }
}
