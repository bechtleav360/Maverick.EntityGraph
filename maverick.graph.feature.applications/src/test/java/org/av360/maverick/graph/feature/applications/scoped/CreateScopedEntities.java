package org.av360.maverick.graph.feature.applications.scoped;

import lombok.extern.slf4j.Slf4j;
import org.av360.maverick.graph.feature.applications.config.ApplicationsTestsBase;
import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;
import org.av360.maverick.graph.model.vocabulary.ICAL;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
@AutoConfigureWebTestClient(timeout = "360000")
@Slf4j
public class CreateScopedEntities  extends ApplicationsTestsBase {




    @AfterEach
    public void resetRepository() {
        super.resetRepository("test_app");
    }

    @Test
    public void createEntity() {
        super.printStart("creating scoped entity");
        applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true))
                .expectStatus().isCreated();


        Resource file = new ClassPathResource("requests/create-valid.jsonld");

        super.printStep();
        RdfConsumer c1 = entitiesTestClient.createEntity(file, "/api/entities", Map.of("X-Application", "test_app"));

        super.printStep();


        super.dumpStatementsAsTable(Map.of("X-Application", "test_app"));

        super.printStep();
        RdfConsumer c2 = entitiesTestClient.listEntities("/api/s/test_app/entities");
        Assertions.assertEquals(1, c2.getStatements().size());
        Assertions.assertEquals("test_app.78q5z6xx", c2.getEntityIdentifier(ICAL.VEVENT).getLocalName());
        super.printStep();
        RdfConsumer c3 = entitiesTestClient.listEntities("/api/entities");
        Assertions.assertEquals(0, c3.getStatements().size());





        super.resetRepository();
        // check if correct application events have been recorded
    }

}
