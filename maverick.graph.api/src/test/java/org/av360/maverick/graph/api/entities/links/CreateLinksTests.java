package org.av360.maverick.graph.api.entities.links;

import io.av360.maverick.graph.model.vocabulary.SDO;
import io.av360.maverick.graph.tests.config.TestConfigurations;
import io.av360.maverick.graph.tests.generator.EntitiesGenerator;
import io.av360.maverick.graph.tests.util.RdfConsumer;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.av360.maverick.graph.api.clients.TestClient;
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
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
public class CreateLinksTests extends TestsBase {

    @Autowired
    private WebTestClient webClient;
    private TestClient client;

    @BeforeEach
    public void setup() {

        client = new TestClient(super.webClient);
    }


    @Test
    void createLink() {
        RdfConsumer rc1 = client.createEntity(EntitiesGenerator.generateCreativeWork());
        Resource source = rc1.findStatement(null, RDF.TYPE, SDO.CREATIVE_WORK).getSubject();
        Assertions.assertTrue(source.isIRI());
        String sourceIdentifier = ((IRI) source).getLocalName();

        RdfConsumer rc2 =  client.createEntity(EntitiesGenerator.generateDefinedTerm());
        Resource target = rc2.findStatement(null, RDF.TYPE, SDO.DEFINED_TERM).getSubject();
        Assertions.assertTrue(target.isIRI());
        String targetIdentifier = ((IRI) target).getLocalName();

        client.createLink(sourceIdentifier, "sdo.hasDefinedTerm", targetIdentifier)
                .expectStatus().isCreated();

    }

}
