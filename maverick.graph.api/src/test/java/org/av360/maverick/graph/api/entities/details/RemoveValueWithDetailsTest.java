package org.av360.maverick.graph.api.entities.details;

import org.av360.maverick.graph.model.vocabulary.SDO;
import org.av360.maverick.graph.services.api.values.ValuesUtils;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.av360.maverick.graph.tests.generator.EntitiesGenerator;
import org.av360.maverick.graph.tests.util.ApiTestsBase;
import org.av360.maverick.graph.tests.util.CsvConsumer;
import org.av360.maverick.graph.tests.util.RdfConsumer;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class RemoveValueWithDetailsTest extends ApiTestsBase  {


    @Test
    public void purgeDetailsByRemovingValue() {
        super.printStart("Remove a value and all its details.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value 'sdo.teaches'");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.source", "source value", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.teaches", "dc.subject", "text value", null);

        super.printStep("Dumping current model");
        CsvConsumer cc1 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc1);
        Assertions.assertEquals(12, cc1.getRows().size());

        super.printStep("Removing value 'sdo.teaches'");
        super.getTestClient().deleteValue(sourceIdentifier, "sdo.teaches");

        super.printStep("Dumping current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(5, cc2.getRows().size());
    }

    @Test
    public void purgeDetailsByRemovingValueWithMultipleValues() {
        super.printStart("Remove a value and all its details.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value 'sdo.teaches'");
        super.getTestClient().createValue(sourceIdentifier, "sdo.propA", "skill");

        super.printStep("Setting value 'sdo.competencyRequired'");
        super.getTestClient().createValue(sourceIdentifier, "sdo.propB", "skill");

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propA", "eav.detailA", "A - first detail", null);

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propB", "eav.detailA", "B - first detail", null);

        super.printStep("Dumping current model");
        CsvConsumer cc1 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc1);
        Assertions.assertEquals(17, cc1.getRows().size());

        super.printStep("Removing value 'sdo.propA'");
        super.getTestClient().deleteValue(sourceIdentifier, "sdo.propA");

        super.printStep("Dumping current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(11, cc2.getRows().size());
    }

    @Test
    public void purgeDetailsForEntityWithMultipleValuesForTheSameProperty() {
        super.printStart("Remove a value and all its details.");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value 'sdo.teaches'");
        super.getTestClient().createValue(sourceIdentifier, "sdo.propA", "skill");

        super.printStep("Setting value 'sdo.competencyRequired'");
        super.getTestClient().createValue(sourceIdentifier, "sdo.propB", "skill 1");
        super.getTestClient().createValue(sourceIdentifier, "sdo.propB", "skill 2", false);

        super.printStep("Setting details");

        super.getTestClient().addDetail(sourceIdentifier, "sdo.propA", "eav.detailA", "A - first detail", null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propA", "eav.detailB", "A - second detail", null);


        super.printStep("Setting details");
        String hash1 = ValuesUtils.generateHashForValue("https://schema.org/propB", "skill 1");
        String hash2 = ValuesUtils.generateHashForValue("https://schema.org/propB", "skill 2");

        super.getTestClient().addDetail(sourceIdentifier, "sdo.propB", "eav.detailA", "B1 - first detail", hash1, null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propB", "eav.detailB", "B1 - second detail", hash1,  null);

        super.printStep("Setting details");
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propB", "eav.detailA", "B2 - first detail", hash2, null);
        super.getTestClient().addDetail(sourceIdentifier, "sdo.propB", "eav.detailB", "B2 - second detail", hash2, null);

        super.printStep("Dumping current model");
        CsvConsumer cc1 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc1);
        Assertions.assertEquals(26, cc1.getRows().size());

        super.printStep("Removing value 'eav.propB'");
        super.getTestClient().deleteValueByHash(sourceIdentifier, "sdo.propB", hash2);

        super.printStep("Dumping current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(19, cc2.getRows().size());
    }


    @BeforeEach
    public void resetRepository() {
        super.resetRepository();
    }

}
