package org.av360.maverick.graph.api.entities.details;

import org.av360.maverick.graph.model.vocabulary.DCTERMS;
import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.Values;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class CreateDetailsTests extends ApiTestsBase  {
    @Test
    @Disabled
    public void createDetailDefault() {

        super.printStart("Add a detail to a single value");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");

        super.printStep("Setting detail");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr", null);

        super.printStep("Retrieving all values");
        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier);
        rc2.print(RDFFormat.TURTLESTAR);

        ValueFactory vf = SimpleValueFactory.getInstance();

        Assertions.assertTrue(rc2.hasStatement(sourceIdentifier, SDO.TEACHES, Values.literal("a certain skill", "en")));
        Assertions.assertTrue(rc2.hasStatement(Values.triple(vf, sourceIdentifier, SDO.TEACHES, Values.literal("a certain skill", "en")), DCTERMS.SOURCE, Values.literal("zephyr")));
    }


    @Test
    @Disabled
    public void createDetailMultipleFail() {

        super.printStart("Add a detail to a properties with multiple values. Has to fail.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "another skill", false);



        super.printStep("Setting detail");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr", null).expectStatus().isBadRequest();
    }


    @Test
    @Disabled
    public void createDetailMultipleWithHash() {

        super.printStart("Add a detail to a properties with multiple values, identify selected value by hash.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "s1");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "s2", false);

        super.printStep("Retrieving all values");
        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier);
        rc2.print(RDFFormat.TURTLESTAR);
        Statement statement = rc2.findStatement(Values.triple(vf, sourceIdentifier, SDO.TEACHES, Values.literal("s1", "en")), org.eclipse.rdf4j.model.vocabulary.DCTERMS.IDENTIFIER, null);
        String hash = statement.getObject().stringValue();


        super.printStep("Setting detail with hash %s".formatted(hash));
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "s1", hash, null).expectStatus().isOk();
    }


    @Test
    @Disabled
    public void createMultipleDetails() {

        super.printStart("Add a detail to a properties with multiple values, identify selected value by hash.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "source", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.subject", "text", null);

        super.printStep("Retrieving all values");
        RdfConsumer rc2 = super.getTestClient().listValues(sourceIdentifier);
        rc2.print(RDFFormat.TURTLESTAR);


        Assertions.assertTrue(rc2.hasStatement(sourceIdentifier, SDO.TEACHES, Values.literal("skill", "en")));
        Assertions.assertTrue(rc2.hasStatement(Values.triple(vf, sourceIdentifier, SDO.TEACHES, Values.literal("skill", "en")), DCTERMS.SOURCE, Values.literal("source")));
        Assertions.assertTrue(rc2.hasStatement(Values.triple(vf, sourceIdentifier, SDO.TEACHES, Values.literal("skill", "en")), DCTERMS.SUBJECT, Values.literal("text")));

    }


    @Test
    @Disabled
    public void createMultipleDetailsDuplicateFail() {

        super.printStart("Add a detail to a properties with multiple values, identify selected value by hash.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "generated text", null);
    }


    public void resetRepository() {
        super.resetRepository();
    }
}
