package org.av360.maverick.graph.api.entities.values;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
/**
 * Tests for endpoint POST /api/rs/{id}/{prefix.key}
 */
public class ListValuesTest extends ApiTestsBase {

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }


    @Test
    public void listAllValues() {

        super.printStart("List all values for an entity");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");

        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier);
        rc2.print(RDFFormat.TURTLESTAR);

        Assertions.assertEquals(10, rc2.asModel().size());
    }


    @Test
    public void listValuesForProperty() {

        super.printStart("List values for a specific property");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);
        super.getTestClient().createValue(sourceIdentifier, "sdo.publisher", "author three", false);


        RdfConsumer rdfConsumer2 = super.getTestClient().listValues(sourceIdentifier, "sdo.author");
        rdfConsumer2.print(RDFFormat.TURTLESTAR);

        Assertions.assertEquals(2, rdfConsumer2.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());
    }
}
