package org.av360.maverick.graph.feature.applications.scoped;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.client.ApplicationsTestClient;
import org.av360.maverick.graph.feature.applications.services.model.ApplicationFlags;
import org.av360.maverick.graph.tests.clients.EntitiesTestClient;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
@AutoConfigureWebTestClient(timeout = "360000")
@Slf4j
public class ReadScopedEntities extends ApiTestsBase {
    @Autowired
    private WebTestClient webClient;

    private ApplicationsTestClient client;
    private EntitiesTestClient entityClient;

    @BeforeEach
    public void setup() {
        client = new ApplicationsTestClient(super.webClient);
        entityClient = new EntitiesTestClient(super.webClient);

    }

    @Test
    public void createEntity() {
        super.printStart("reading scoped entity");
        client.createApplication("test_app", new ApplicationFlags(false, true)).expectStatus().isCreated();


        Resource file = new ClassPathResource("requests/create-valid.ttl");

        super.printStep();
        RdfConsumer c1 = this.entityClient.createEntity(file, "/api/entities", Map.of("X-Application", "test_app"));

        super.printStep();


        super.dump(Map.of("X-Application", "test_app"));

        super.printStep();
        RdfConsumer c2 = this.entityClient.listEntities("/api/s/test_app/entities");
        Assertions.assertEquals(2, c2.getStatements().size());
    }

}
