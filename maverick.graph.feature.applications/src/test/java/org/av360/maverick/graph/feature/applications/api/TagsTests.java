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
import org.av360.maverick.graph.feature.applications.services.ApplicationsService;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;

import java.util.List;
import java.util.Set;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestSecurityConfig.class)
@RecordApplicationEvents
@ActiveProfiles({"test", "api"})
public class TagsTests extends ApplicationsTestsBase  {
    @BeforeAll
    static void before() {
        ApplicationsService.APPLICATION_CACHING = false;
    }


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void addTagToApplication() {

        super.printStep("Creating applications");

        super.applicationsTestClient.createApplication("test_1_app_0", new ApplicationFlags(false, true), Set.of())
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class);
        super.applicationsTestClient.createApplication("test_1_app_1", new ApplicationFlags(false, true), Set.of("t1"))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class);

        Responses.ApplicationResponse createResponse = super.applicationsTestClient.createApplication("test_app_2", new ApplicationFlags(false, true), Set.of("t1"))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();
        Assertions.assertNotNull(createResponse);


        super.printStep("Adding tag to application");

        Responses.ApplicationResponse createTagResponse = super.applicationsTestClient.addApplicationTag(createResponse.key(), "t2")
                .expectStatus().isOk()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();
        Assertions.assertNotNull(createTagResponse);


        super.printStep("Listing all applications with tag: t1");
        List<Responses.ApplicationResponse> filteredListResponse2 = super.applicationsTestClient.listApplications("t1")
                .expectStatus().isOk()
                .expectBodyList(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(filteredListResponse2);
        Assertions.assertEquals(2, filteredListResponse2.size());

        super.printStep("Listing all applications with tag: t2");
        List<Responses.ApplicationResponse> filteredListResponse3 = super.applicationsTestClient.listApplications("t2")
                .expectStatus().isOk()
                .expectBodyList(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(filteredListResponse3);
        Assertions.assertEquals(1, filteredListResponse3.size());


    }

    @Test
    public void createApplicationWithTag() {

        super.printStep("Creating application");

        Responses.ApplicationResponse createResponse = super.applicationsTestClient.createApplication("test_2_app", new ApplicationFlags(false, true), Set.of("t3"))
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
    public void filterApplications() {

        super.printStep("Creating applications");

        super.applicationsTestClient.createApplication("test_3_app_0", new ApplicationFlags(false, true), Set.of("t4"))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.applicationsTestClient.createApplication("test_3_app_1", new ApplicationFlags(true, true), Set.of("t4"))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.applicationsTestClient.createApplication("test_3_app_2", new ApplicationFlags(false, false))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep("List all applications");



        List<Responses.ApplicationResponse> filteredListResponse = super.applicationsTestClient.listApplications("t4")
                .expectStatus().isOk()
                .expectBodyList(Responses.ApplicationResponse.class)
                .returnResult().getResponseBody();

        Assertions.assertNotNull(filteredListResponse);
        Assertions.assertEquals(2, filteredListResponse.size());



    }

}
