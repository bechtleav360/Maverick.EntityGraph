package org.av360.maverick.graph.api.entities.links;

import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.tests.clients.TestEntitiesClient;
import io.av360.maverick.graph.tests.config.TestSecurityConfig;
import io.av360.maverick.graph.tests.generator.EntitiesGenerator;
import io.av360.maverick.graph.tests.util.ApiTestsBase;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
public class RemoveLinkTests extends ApiTestsBase {

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
    void deleteLink() {
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        String sourceIdentifier = rc1.getEntityKey(SDO.CREATIVE_WORK);

        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        String targetIdentifier = rc2.getEntityKey(SDO.DEFINED_TERM);

        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();


        client.deleteLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isOk();

    }


    @Test
    void deleteUnknownLink() {
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        Resource source = rc1.findStatement(null, RDF.TYPE, SDO.CREATIVE_WORK).getSubject();
        Assertions.assertTrue(source.isIRI());
        String sourceIdentifier = ((IRI) source).getLocalName();

        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource target = rc2.findStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String targetIdentifier = ((IRI) target).getLocalName();

        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();


        client.deleteLink(sourceIdentifier, "sdo.teaches", targetIdentifier)
                .expectStatus().isOk();
    }

    @Test
    void deleteForUnknownEntity() {
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        Resource source = rc1.findStatement(null, RDF.TYPE, SDO.CREATIVE_WORK).getSubject();
        Assertions.assertTrue(source.isIRI());
        String sourceIdentifier = ((IRI) source).getLocalName();

        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource target = rc2.findStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String targetIdentifier = ((IRI) target).getLocalName();

        RdfConsumer rc3 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource another = rc2.findStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String anotherIdentifier = ((IRI) target).getLocalName();

        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();

        client.deleteLink(anotherIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isOk();

        // TODO: check in transaction that nothing changed
    }


}
