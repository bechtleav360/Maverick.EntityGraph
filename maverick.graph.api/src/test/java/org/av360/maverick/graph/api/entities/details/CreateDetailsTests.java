package org.av360.maverick.graph.api.entities.details;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
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
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr");
    }


    @Test
    @Disabled
    public void createDetailMultipleFail() {

        super.printStart("Add a detail to a properties with multiple values. Has to fail.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "another skill");

        super.printStep("Setting detail");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr");
    }


    @Test
    @Disabled
    public void createDetailMultipleWithHash() {

        super.printStart("Add a detail to a properties with multiple values, identify selected value by hash.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "another skill");

        super.printStep("Setting detail");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr", "hash");
    }


    @Test
    @Disabled
    public void createMultipleDetails() {

        super.printStart("Add a detail to a properties with multiple values, identify selected value by hash.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.subject", "generated text");
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
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "zephyr");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "generated text");
    }


    public void resetRepository() {
        super.resetRepository();
    }
}
