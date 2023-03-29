package org.av360.maverick.graph.api.entities.links;

import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.tests.clients.TestEntitiesClient;
import io.av360.maverick.graph.tests.config.TestSecurityConfig;
import io.av360.maverick.graph.tests.generator.EntitiesGenerator;
import io.av360.maverick.graph.tests.generator.GeneratorCommons;
import io.av360.maverick.graph.tests.util.ApiTestsBase;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class CreateLinksTests extends ApiTestsBase {

    @Autowired
    private WebTestClient webClient;
    private TestEntitiesClient client;

    @BeforeEach
    public void setup() {
        client = new TestEntitiesClient(super.webClient);
    }

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }


    @Test
    void createLink() {

        super.printStart("createLink");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep();
        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        IRI targetIdentifier = rc2.getEntityIdentifier(SDO.DEFINED_TERM);

        super.printStep();
        client.createLink(sourceIdentifier.getLocalName(), "sdo.hasDefinedTerm", targetIdentifier.getLocalName())
                .expectStatus().isCreated();

        super.printStep();
        RdfConsumer rc3 = client.readEntity(sourceIdentifier.getLocalName());
        rc3.print();


        Assertions.assertTrue(rc3.hasStatement(sourceIdentifier, createIRIFrom("https://schema.org/hasDefinedTerm"), targetIdentifier));

    }

    @Test
    void createWithUnknownTarget() {

        super.printStart("createWithUnknownTarget");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        String sourceIdentifier = rc1.getEntityKey(SDO.CREATIVE_WORK);

        String targetIdentifier = GeneratorCommons.generateRandomEntityIdentifier();

        super.printStep();
        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isNotFound();
    }

    @Test
    void createWithUnknownSource() {
        super.printStart("createWithUnknownSource");
        String sourceIdentifier = GeneratorCommons.generateRandomEntityIdentifier();

        String targetIdentifier = GeneratorCommons.generateRandomEntityIdentifier();

        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isNotFound();
    }

    @Test
    void createWithUnknownPrefix() {
        super.printStart("createWithUnknownPrefix");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        String sourceIdentifier = rc1.getEntityKey(SDO.CREATIVE_WORK);

        super.printStep();
        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        String targetIdentifier = rc2.getEntityKey(SDO.DEFINED_TERM);

        super.printStep();
        client.createLink(sourceIdentifier, "xxx.hasDefinedTerm", targetIdentifier)
                .expectStatus().isBadRequest();
    }



}
