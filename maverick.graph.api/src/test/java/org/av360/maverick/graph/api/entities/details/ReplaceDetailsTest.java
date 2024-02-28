/*
 * Copyright (c) 2024.
 *
 *  Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 *  European Commission - subsequent versions of the EUPL (the "Licence");
 *
 *  You may not use this work except in compliance with the Licence.
 *  You may obtain a copy of the Licence at:
 *
 *  https://joinup.ec.europa.eu/software/page/eupl5
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the Licence for the specific language governing permissions and limitations under the Licence.
 *
 */

package org.av360.maverick.graph.api.entities.details;

import org.av360.maverick.graph.model.vocabulary.SDO;
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
public class ReplaceDetailsTest extends ApiTestsBase {

    @BeforeEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void createDetailDefault() {
        super.printStart("Add a detail to a single value");

        RdfConsumer rc1 = super.getTestClient().createEntity(EntitiesGenerator.generateCreativeWork());
        IRI sourceIdentifier = rc1.getEntityIdentifier(SDO.CREATIVE_WORK);

        super.printStep("Setting value");
        super.getTestClient().createValue(sourceIdentifier, "sdo.teaches", "a certain skill");

        super.printStep("Setting detail");
        super.getTestClient().setDetail(sourceIdentifier, "sdo.teaches", "eav.status", "suggested", null);

        super.printStep("Setting detail");
        super.getTestClient().setDetail(sourceIdentifier, "sdo.teaches", "eav.status", "approved", null);

        super.printStep("Dumping and validating current model");
        CsvConsumer cc2 = super.getTestClient().listAllStatements();
        super.dumpStatementsAsTable(cc2);
        Assertions.assertEquals(12, cc2.getRows().size());

    }
}
