package org.av360.maverick.graph.api.entities.links;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.clients.EntitiesTestClient;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.vocabulary.RDF;
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
    private EntitiesTestClient client;

    @BeforeEach
    public void setup() {

        client = new EntitiesTestClient(super.webClient);
        super.resetRepository();
    }



    @Test
    void deleteLink() {
        super.printStart("deleteLink");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        String sourceIdentifier = rc1.getEntityKey(SDO.CREATIVE_WORK);

        super.printStep();
        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        String targetIdentifier = rc2.getEntityKey(SDO.DEFINED_TERM);

        super.printStep();
        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();

        super.printStep();
        client.deleteLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isOk();

    }


    @Test
    void deleteUnknownLink() {
        super.printStart("deleteUnknownLink");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        Resource source = rc1.findFirstStatement(null, RDF.TYPE, SDO.CREATIVE_WORK).getSubject();
        Assertions.assertTrue(source.isIRI());
        String sourceIdentifier = ((IRI) source).getLocalName();

        super.printStep();
        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource target = rc2.findFirstStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String targetIdentifier = ((IRI) target).getLocalName();

        super.printStep();
        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();

        super.printStep();
        client.deleteLink(sourceIdentifier, "sdo.teaches", targetIdentifier)
                .expectStatus().isOk();
    }

    @Test
    void deleteForUnknownEntity() {
        super.printStart("deleteForUnknownEntity");
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        Resource source = rc1.findFirstStatement(null, RDF.TYPE, SDO.CREATIVE_WORK).getSubject();
        Assertions.assertTrue(source.isIRI());
        String sourceIdentifier = ((IRI) source).getLocalName();

        super.printStep();
        RdfConsumer rc2 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource target = rc2.findFirstStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String targetIdentifier = ((IRI) target).getLocalName();

        super.printStep();
        RdfConsumer rc3 = client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource another = rc2.findFirstStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String anotherIdentifier = ((IRI) target).getLocalName();

        super.printStep();
        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();

        super.printStep();
        client.deleteLink(anotherIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isOk();

        // TODO: check in transaction that nothing changed
    }


}
