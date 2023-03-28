package io.av360.maverick.graph.feature.applications.scoped;

import io.av360.maverick.graph.feature.applications.client.ApplicationsTestClient;
import io.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import io.av360.maverick.graph.tests.clients.TestEntitiesClient;
import io.av360.maverick.graph.tests.config.TestSecurityConfig;
import io.av360.maverick.graph.tests.util.ApiTestsBase;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
@Slf4j
public class CreateScopedEntities  extends ApiTestsBase {
    @Autowired
    private WebTestClient webClient;

    private ApplicationsTestClient client;
    private TestEntitiesClient entityClient;

    @BeforeEach
    public void setup() {
        client = new ApplicationsTestClient(super.webClient);
        entityClient = new TestEntitiesClient(super.webClient);

    }

    @Test
    public void createEntity() {
        client.createApplication("testApp", new ApplicationFlags(false, true))
                .expectStatus().isCreated();


        Resource file = new ClassPathResource("requests/create-valid.jsonld");

        log.info("----------------------------------");
        RdfConsumer c1 = this.entityClient.createEntity(file, "/api/app/testApp/entities");

        log.info("----------------------------------");

        RdfConsumer c2 = this.entityClient.listEntities("/api/app/testApp/entities");
        Assertions.assertEquals(4, c2.getStatements().size());

        log.info("----------------------------------");
        RdfConsumer c3 = this.entityClient.listEntities("/api/entities");
        Assertions.assertEquals(0, c3.getStatements().size());






        // check if correct application events have been recorded
    }

}
