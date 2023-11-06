package org.av360.maverick.graph.api.entities.details;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.CsvConsumer;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
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
public class RemoveDetailsTests extends ApiTestsBase  {
    @Test
    public void deleteDetail() {
        super.printStart("Remove a specific detail.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "source", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.subject", "text", null);

        super.printStep("Dumping and validating current model");
        CsvConsumer cc1 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc1);
        Assertions.assertEquals(7, cc1.getRows().size());

        super.printStep("Deleting detail dc.source from predicate teaches");
        super.getTestClient().deleteValueDetail(sourceIdentifier, "sdo.teaches", "dc.source").expectStatus().isOk();

        super.printStep("Dumping and validating current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(6, cc2.getRows().size());
    }

    @Test
    public void purgeDetailsByRemovingValue() {
        super.printStart("Remove a value and all its details.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "source value", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.subject", "text value", null);

        super.printStep("Dumping current model");
        CsvConsumer cc1 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc1);
        Assertions.assertEquals(7, cc1.getRows().size());

        super.printStep("Removing value");
        super.getTestClient().deleteValue(sourceIdentifier, "sdo.teaches");

        super.printStep("Dumping current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(4, cc2.getRows().size());
    }
    @Test
    @Disabled
    public void deleteSpecificDetailByHash() {

    }

    @Test
    @Disabled
    public void deleteSpecificDetailWithoutHashFail() {

    }

}
