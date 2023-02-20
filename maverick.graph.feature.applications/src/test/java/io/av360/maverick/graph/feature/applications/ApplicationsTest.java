package io.av360.maverick.graph.feature.applications;

import io.av360.maverick.graph.feature.applications.api.dto.Requests;
import io.av360.maverick.graph.feature.applications.api.dto.Responses;
import io.av360.maverick.graph.tests.api.v2.Subscriptions;
import io.av360.maverick.graph.tests.util.TestsBase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ContextConfiguration(classes = TestConfigurations.class)
@RecordApplicationEvents
@ActiveProfiles("test")
class ApplicationsTest extends TestsBase implements Subscriptions {

    @Override
    @Test
    public void createSubscription() {
        this.postSubscription("test", false).jsonPath("$.key").isNotEmpty();
    }

    private WebTestClient.BodyContentSpec postSubscription(String label, boolean persistent) {
        Requests.RegisterApplicationRequest req = new Requests.RegisterApplicationRequest(label, persistent);

        return webClient.post()
                .uri("/api/applications")
                .body(BodyInserters.fromValue(req))
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody();
        // .jsonPath("$.key").isNotEmpty();
    }

    @Override
    @Test
    public void listSubscription() {

        super.dump();

        this.postSubscription("a", false).jsonPath("$.key").isNotEmpty();
        this.postSubscription("b", false).jsonPath("$.key").isNotEmpty();
        this.postSubscription("c", false).jsonPath("$.key").isNotEmpty();

        webClient.get()
                .uri("/api/applications")
                .exchange()
                .expectStatus().isOk()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody()
                .consumeWith(entityExchangeResult -> {
                    String s = new String(entityExchangeResult.getResponseBody());
                    Assertions.assertTrue(StringUtils.hasLength(s));
                })
                //.jsonPath("$.identifier").isNotEmpty()

                .jsonPath("$").isArray()
                .jsonPath("$.size()").isEqualTo(3);
    }

    @Override
    @Test
    public void generateKey() {

        Requests.RegisterApplicationRequest req = new Requests.RegisterApplicationRequest("test", false);

        Responses.ApplicationResponse re = webClient.post()
                .uri("/api/applications")
                .bodyValue(req)
                .exchange()
                .expectStatus().isCreated()
                //.expectBody(Responses.CreateSubscriptionResponse.class)
                .expectBody(Responses.ApplicationResponse.class)
                .returnResult()
                .getResponseBody();
        Assertions.assertNotNull(re.key());

        Requests.CreateApiKeyRequest request = new Requests.CreateApiKeyRequest("test");

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/applications/{id}/keys")
                        .build(re.key())
                )
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(Responses.ApiKeyResponse.class);
    }

    @Override
    @Test
    public void revokeToken() {
    }

    @AfterEach
    public void reset() {
        super.resetRepository("application");
    }
}