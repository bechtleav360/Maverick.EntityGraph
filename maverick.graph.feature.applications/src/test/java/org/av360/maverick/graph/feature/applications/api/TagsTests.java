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

package org.av360.maverick.graph.feature.applications.api;

import org.av360.maverick.graph.feature.applications.config.ApplicationsTestsBase;
import org.av360.maverick.graph.feature.applications.controller.dto.Responses;
import org.av360.maverick.graph.feature.applications.model.domain.ApplicationFlags;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class TagsTests extends ApplicationsTestsBase  {



    @AfterEach
    public void resetRepository() {
        super.resetRepository("test_app");
    }


    @Test
    @Disabled
    public void createApplicationWithTag() {

        super.printStep("Creating application");

        Responses.ApplicationResponse createResponse = super.applicationsTestClient.createApplication("test_app", new ApplicationFlags(false, true), Set.of("test"))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();
        Assertions.assertNotNull(createResponse);

        Responses.ApplicationResponse getResponse = super.applicationsTestClient.getApplication(createResponse.key())
                .expectStatus().isOk()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(getResponse);
        Assertions.assertEquals(createResponse.key(), getResponse.key());
        Assertions.assertEquals(1, getResponse.tags().size());


    }

    @Test
    @Disabled
    public void filterApplications() {

        super.printStep("Creating applications");

        super.applicationsTestClient.createApplication("test_app_0", new ApplicationFlags(false, true), Set.of("test"))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.applicationsTestClient.createApplication("test_app_1", new ApplicationFlags(true, true), Set.of("test"))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.applicationsTestClient.createApplication("test_app_2", new ApplicationFlags(false, false))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep("List all applications");

        byte[] sr = super.applicationsTestClient.listApplications()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$").value(obj -> {
                    Assertions.assertNotNull(obj);
                    if(obj instanceof JSONArray array) {
                        Assertions.assertEquals(array.length(), 3);
                    }
                })
                .jsonPath("$.size()").isEqualTo(3)
                .returnResult().getResponseBody();
        super.printResult("Result", new String(sr));
        super.printStep("List all applications with tag");

        super.printResult("Result", new String(sr));


    }

}
