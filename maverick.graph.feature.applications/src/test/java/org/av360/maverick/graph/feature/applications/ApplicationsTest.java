package org.av360.maverick.graph.feature.applications;

import org.av360.maverick.graph.feature.applications.api.dto.Responses;
import org.av360.maverick.graph.feature.applications.client.ApplicationsTestClient;
import org.av360.maverick.graph.feature.applications.config.ApplicationsTestsBase;
import org.av360.maverick.graph.feature.applications.domain.model.ApplicationFlags;
import org.av360.maverick.graph.tests.config.TestSecurityConfig;
import org.junit.jupiter.api.AfterEach;
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
class ApplicationsTest extends ApplicationsTestsBase {

    private ApplicationsTestClient client;

    @BeforeEach
    public void setup() {
        client = new ApplicationsTestClient(super.webClient);
    }


    @AfterEach
    public void resetRepository() {
        super.resetRepository();
    }

    @Test
    public void createPublicApplication() {
        super.printStep();

        this.client.createApplication("test-public", new ApplicationFlags(false, true, null, null, null))
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.key").isNotEmpty();
    }



    @Test
    public void listApplications() {

        super.printStep();

        this.client.createApplication("a", new ApplicationFlags(false, true, null, null, null))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        this.client.createApplication("b", new ApplicationFlags(true, true, null, null, null))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        this.client.createApplication("c", new ApplicationFlags(false, false, null, null, null))
                .expectStatus().isCreated().expectBody().jsonPath("$.key").isNotEmpty();

        super.printStep();

        this.client.listApplications()
                        .expectStatus().isOk()
                        .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.size()").isEqualTo(3);


    }


    @Test
    public void getApplication() {


        Responses.ApplicationResponse app = this.client.createApplication("test", new ApplicationFlags(false, true, null, null, null))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        this.client.getApplication(app.key())
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.key").isEqualTo(app.key());

    }

    @Test
    public void createSubscription() {
        Responses.ApplicationResponse re = this.client.createApplication("test-public", new ApplicationFlags(false, false, null, null, null))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        Assertions.assertNotNull(re);
        Assertions.assertNotNull(re.key());


        this.client.createSubscription("test-subscription", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

    }

    @Test
    public void listSubscriptions() {
        Responses.ApplicationResponse re = this.client.createApplication("test-public", new ApplicationFlags(false, false, null, null, null))
                .expectStatus().isCreated()
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();

        this.client.createSubscription("test-sub-1", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        this.client.createSubscription("test-sub-2", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        this.client.createSubscription("test-sub-3", re.key())
                .expectStatus().isCreated()
                .expectBody(Responses.SubscriptionResponse.class);

        this.client.listSubscriptions(re.key())
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isArray()
                .jsonPath("$.size()").isEqualTo(3);


    }


}