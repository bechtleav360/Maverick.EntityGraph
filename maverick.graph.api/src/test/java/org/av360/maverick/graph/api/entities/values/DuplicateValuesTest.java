package org.av360.maverick.graph.api.entities.values;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
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
public class DuplicateValuesTest extends ApiTestsBase {

    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void addValue() {

        super.printStart("Add a value as list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting author");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");

        RdfConsumer rdfConsumer1 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer1.print();

        super.printStep("Setting another author");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);

        RdfConsumer rdfConsumer2 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer2.print();

        Assertions.assertEquals(2, rdfConsumer2.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());

    }


    @Test
    public void replaceValue() {

        super.printStart("Replace a value");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting author");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");

        RdfConsumer rdfConsumer1 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer1.print();

        super.printStep("Setting another author");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", true);

        RdfConsumer rdfConsumer2 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer2.print();

        Assertions.assertEquals(1, rdfConsumer2.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());

    }

    @Test
    public void listValues() {

        super.printStart("Remove a value from a list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author three", false);





        RdfConsumer rdfConsumer2 = super.getTestClient().listValues(sourceIdentifier, "sdo.author");
        rdfConsumer2.print();

        Assertions.assertEquals(1, rdfConsumer2.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());

    }

    @Test
    public void removeValue() {

        super.printStart("Remove a value from a list");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author one");
        super.getTestClient().createValue(sourceIdentifier, "sdo.author", "author two", false);


        RdfConsumer rdfConsumer1 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer1.print();

        super.printStep("Remove the second author");



        RdfConsumer rdfConsumer2 = super.getTestClient().readEntity(sourceIdentifier);
        rdfConsumer2.print();

        Assertions.assertEquals(1, rdfConsumer2.asModel().filter(sourceIdentifier, SDO.AUTHOR, null).size());

    }

}
